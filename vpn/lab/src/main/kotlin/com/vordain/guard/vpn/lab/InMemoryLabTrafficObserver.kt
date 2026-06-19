package com.vordain.guard.vpn.lab

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.policy.DefaultPolicyEngine
import com.vordain.guard.core.policy.Policy
import com.vordain.guard.vpn.classifier.StaticRuleListClassifier
import com.vordain.guard.vpn.dns.DnsMessageParser
import com.vordain.guard.vpn.dns.DnsParseResult
import com.vordain.guard.vpn.engine.DefaultDomainTrafficEvaluator
import com.vordain.guard.vpn.engine.DomainTrafficEvaluator
import com.vordain.guard.vpn.engine.TrafficAction
import com.vordain.guard.vpn.packet.IpPacketParser
import com.vordain.guard.vpn.packet.PacketParseResult

class InMemoryLabTrafficObserver(
    private val packetParser: IpPacketParser = IpPacketParser(),
    private val dnsParser: DnsMessageParser = DnsMessageParser(),
    private val domainTrafficEvaluator: DomainTrafficEvaluator = defaultDomainTrafficEvaluator(),
    private val policy: Policy = defaultLabPolicy(),
    private val recentObservationLimit: Int = DEFAULT_RECENT_OBSERVATION_LIMIT,
) : LabTrafficObserver {
    private val lock = Any()
    private var stats = LabTrafficObservationStats()

    override fun reset(startedAtMillis: Long) {
        synchronized(lock) {
            stats = LabTrafficObservationStats(lastPacketSummary = "Lab capture started at $startedAtMillis")
        }
    }

    override fun observePacket(
        packet: ByteArray,
        length: Int,
        observedAtMillis: Long,
    ): LabTrafficObservationStats {
        val parseResult = packetParser.parse(packet, length)
        return synchronized(lock) {
            val baseStats = stats.recordPacket(parseResult)
            stats = if (parseResult.metadata.malformed) {
                baseStats
            } else {
                observeDnsIfPresent(baseStats, parseResult, observedAtMillis)
            }
            stats
        }
    }

    override fun markStopped(stoppedAtMillis: Long): LabTrafficObservationStats {
        return synchronized(lock) {
            stats = stats.copy(lastPacketSummary = "Lab capture stopped at $stoppedAtMillis")
            stats
        }
    }

    override fun snapshot(): LabTrafficObservationStats {
        return synchronized(lock) { stats }
    }

    private fun observeDnsIfPresent(
        currentStats: LabTrafficObservationStats,
        parseResult: PacketParseResult,
        observedAtMillis: Long,
    ): LabTrafficObservationStats {
        val udpPayload = parseResult.udpPayload ?: return currentStats
        val sourcePort = parseResult.metadata.sourcePort
        val destinationPort = parseResult.metadata.destinationPort
        if (sourcePort != DNS_PORT && destinationPort != DNS_PORT) {
            return currentStats
        }

        return when (val dnsResult = dnsParser.parse(udpPayload.payload)) {
            is DnsParseResult.Failure -> currentStats.copy(
                dnsPacketCount = currentStats.dnsPacketCount + 1,
                malformedDnsCount = currentStats.malformedDnsCount + 1,
                lastPacketSummary = "Malformed DNS packet: ${dnsResult.reason}",
            )
            is DnsParseResult.Success -> {
                val decisions = dnsResult.questions.map { question ->
                    val decision = domainTrafficEvaluator.evaluateDomain(question.domain, policy)
                    LabDnsDomainDecision(
                        domain = question.domain,
                        actionLabel = decision.action.name,
                        reasonLabel = decision.evaluation.reason.name,
                        shouldCreateEvent = decision.evaluation.shouldCreateEvent,
                    )
                }
                val observation = LabDnsObservation(
                    observedAtMillis = observedAtMillis,
                    queriedDomains = dnsResult.questions.map { it.domain },
                    packetMetadata = parseResult.metadata,
                    decisions = decisions,
                )
                currentStats.copy(
                    dnsPacketCount = currentStats.dnsPacketCount + 1,
                    dnsQueryCount = currentStats.dnsQueryCount + dnsResult.questions.size,
                    allowedDomainCount = currentStats.allowedDomainCount +
                        decisions.count { it.actionLabel == TrafficAction.ALLOW.name },
                    blockedDomainCount = currentStats.blockedDomainCount +
                        decisions.count { it.actionLabel == TrafficAction.BLOCK.name },
                    alertOnlyDomainCount = currentStats.alertOnlyDomainCount +
                        decisions.count { it.actionLabel == TrafficAction.ALERT_ONLY.name },
                    lastDnsObservation = observation,
                    recentDnsObservations = (listOf(observation) + currentStats.recentDnsObservations)
                        .take(recentObservationLimit),
                    lastPacketSummary = observation.summary(),
                )
            }
        }
    }

    private fun LabTrafficObservationStats.recordPacket(parseResult: PacketParseResult): LabTrafficObservationStats {
        return copy(
            packetCount = packetCount + 1,
            byteCount = byteCount + parseResult.metadata.byteLength,
            malformedPacketCount = malformedPacketCount + if (parseResult.metadata.malformed) 1 else 0,
            lastPacketSummary = parseResult.metadata.summary(),
        )
    }

    companion object {
        const val DNS_PORT = 53
        private const val DEFAULT_RECENT_OBSERVATION_LIMIT = 10

        fun defaultLabPolicy(): Policy {
            return Policy(
                id = PolicyId("lab-debug-policy"),
                mode = LockdownMode.STANDARD,
                allowedDomains = setOf(DomainName.from("allowed.example")),
                blockedDomains = setOf(DomainName.from("blocked.example")),
                allowedPackages = emptySet<AppPackageName>(),
                blockedPackages = emptySet<AppPackageName>(),
                blockUnknownDomains = false,
                blockKnownProxyDomains = true,
            )
        }

        fun defaultDomainTrafficEvaluator(): DomainTrafficEvaluator {
            return DefaultDomainTrafficEvaluator(
                domainClassifier = StaticRuleListClassifier(
                    proxyAnonymizerRules = setOf(DomainName.from("proxy.test")),
                ),
                policyEngine = DefaultPolicyEngine(),
            )
        }
    }
}
