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
import com.vordain.guard.vpn.packet.DnsBlockResponseMode
import com.vordain.guard.vpn.packet.DnsUdpResponseBuilder
import com.vordain.guard.vpn.packet.IpPacketParser
import com.vordain.guard.vpn.packet.PacketParseResult

class InMemoryLabTrafficObserver(
    private val packetParser: IpPacketParser = IpPacketParser(),
    private val dnsParser: DnsMessageParser = DnsMessageParser(),
    private val dnsResponseBuilder: DnsUdpResponseBuilder = DnsUdpResponseBuilder(),
    private val domainTrafficEvaluator: DomainTrafficEvaluator = defaultDomainTrafficEvaluator(),
    private val policy: Policy = defaultLabPolicy(),
    private val recentObservationLimit: Int = DEFAULT_RECENT_OBSERVATION_LIMIT,
    private val dnsBlockResponseMode: DnsBlockResponseMode = DnsBlockResponseMode.NXDOMAIN,
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
    ): LabTrafficObservationStats = handlePacket(packet, length, observedAtMillis).stats

    override fun handlePacket(
        packet: ByteArray,
        length: Int,
        observedAtMillis: Long,
    ): LabPacketHandlingResult {
        val parseResult = packetParser.parse(packet, length)
        return synchronized(lock) {
            val baseStats = stats.recordPacket(parseResult)
            val result = if (parseResult.metadata.malformed) {
                stats = baseStats
                LabPacketHandlingResult(
                    action = LabPacketAction.DROP,
                    responseBytes = null,
                    stats = stats,
                    decisionSummary = parseResult.metadata.summary(),
                )
            } else {
                observeDnsIfPresent(
                    currentStats = baseStats,
                    parseResult = parseResult,
                    originalPacket = packet,
                    originalLength = length,
                    observedAtMillis = observedAtMillis,
                )
            }
            result
        }
    }

    override fun markDnsResponseWriteFailure(observedAtMillis: Long): LabTrafficObservationStats {
        return synchronized(lock) {
            stats = stats.copy(
                dnsResponseWriteFailureCount = stats.dnsResponseWriteFailureCount + 1,
                lastPacketSummary = "DNS block response write failed at $observedAtMillis",
            )
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
        originalPacket: ByteArray,
        originalLength: Int,
        observedAtMillis: Long,
    ): LabPacketHandlingResult {
        val udpPayload = parseResult.udpPayload ?: return drop(currentStats, parseResult.metadata.summary())
        val sourcePort = parseResult.metadata.sourcePort
        val destinationPort = parseResult.metadata.destinationPort
        if (sourcePort != DNS_PORT && destinationPort != DNS_PORT) {
            return drop(currentStats, "Non-DNS packet dropped")
        }

        return when (val dnsResult = dnsParser.parse(udpPayload.payload)) {
            is DnsParseResult.Failure -> {
                stats = currentStats.copy(
                    dnsPacketCount = currentStats.dnsPacketCount + 1,
                    malformedDnsCount = currentStats.malformedDnsCount + 1,
                    lastPacketSummary = "Malformed DNS packet: ${dnsResult.reason}",
                )
                drop(stats, "Malformed DNS packet: ${dnsResult.reason}")
            }
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
                val allowedCount = decisions.count { it.actionLabel == TrafficAction.ALLOW.name }
                val blockedCount = decisions.count { it.actionLabel == TrafficAction.BLOCK.name }
                val alertOnlyCount = decisions.count { it.actionLabel == TrafficAction.ALERT_ONLY.name }
                val observedStats = currentStats.copy(
                    dnsPacketCount = currentStats.dnsPacketCount + 1,
                    dnsQueryCount = currentStats.dnsQueryCount + dnsResult.questions.size,
                    allowedDomainCount = currentStats.allowedDomainCount + allowedCount,
                    blockedDomainCount = currentStats.blockedDomainCount + blockedCount,
                    alertOnlyDomainCount = currentStats.alertOnlyDomainCount + alertOnlyCount,
                    lastDnsObservation = observation,
                    recentDnsObservations = (listOf(observation) + currentStats.recentDnsObservations)
                        .take(recentObservationLimit),
                    lastPacketSummary = observation.summary(),
                )
                if (blockedCount > 0) {
                    val response = dnsResponseBuilder.buildBlockedDnsResponse(
                        packet = originalPacket,
                        length = originalLength,
                        responseMode = dnsBlockResponseMode,
                    )
                    stats = observedStats.copy(
                        dnsBlockedResponseCount = observedStats.dnsBlockedResponseCount + if (response.built) 1 else 0,
                        lastPacketSummary = "${observation.summary()}\n${response.reason}",
                    )
                    if (response.built) {
                        LabPacketHandlingResult(
                            action = LabPacketAction.WRITE_DNS_BLOCK_RESPONSE,
                            responseBytes = response.responseBytes,
                            stats = stats,
                            decisionSummary = "Blocked DNS query; synthetic response planned",
                        )
                    } else {
                        drop(stats, "Blocked DNS query dropped; ${response.reason}")
                    }
                } else {
                    stats = observedStats.copy(
                        dnsAllowedDroppedCount = observedStats.dnsAllowedDroppedCount + allowedCount,
                        dnsAlertDroppedCount = observedStats.dnsAlertDroppedCount + alertOnlyCount,
                    )
                    val reason = when {
                        allowedCount > 0 -> "Allowed DNS forwarding not implemented yet; packet dropped"
                        alertOnlyCount > 0 -> "Alert-only DNS forwarding not implemented yet; packet dropped"
                        else -> "DNS query had no actionable decision; packet dropped"
                    }
                    drop(stats, reason)
                }
            }
        }
    }

    private fun drop(
        currentStats: LabTrafficObservationStats,
        reason: String,
    ): LabPacketHandlingResult {
        stats = currentStats
        return LabPacketHandlingResult(
            action = LabPacketAction.DROP,
            responseBytes = null,
            stats = stats,
            decisionSummary = reason,
        )
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
