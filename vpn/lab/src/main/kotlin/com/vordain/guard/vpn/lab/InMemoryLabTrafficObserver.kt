package com.vordain.guard.vpn.lab

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.intelligence.EncryptedDnsResolverSeedList
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
import com.vordain.guard.vpn.packet.DnsUdpUpstreamResponseWrapper
import com.vordain.guard.vpn.packet.IpPacketParser
import com.vordain.guard.vpn.packet.PacketParseResult

class InMemoryLabTrafficObserver(
    private val packetParser: IpPacketParser = IpPacketParser(),
    private val dnsParser: DnsMessageParser = DnsMessageParser(),
    private val dnsResponseBuilder: DnsUdpResponseBuilder = DnsUdpResponseBuilder(),
    private val dnsUpstreamResponseWrapper: DnsUdpUpstreamResponseWrapper = DnsUdpUpstreamResponseWrapper(),
    private val domainTrafficEvaluator: DomainTrafficEvaluator = defaultDomainTrafficEvaluator(),
    private val encryptedDnsResolverSeedList: EncryptedDnsResolverSeedList = EncryptedDnsResolverSeedList(),
    initialPolicy: Policy = defaultLabPolicy(),
    private val recentObservationLimit: Int = DEFAULT_RECENT_OBSERVATION_LIMIT,
    private val dnsBlockResponseMode: DnsBlockResponseMode = DnsBlockResponseMode.NXDOMAIN,
    initialForwardingMode: LabDnsForwardingMode = LabDnsForwardingMode.DISABLED,
    initialUpstreamTransport: LabDnsUpstreamTransport = NoConfiguredLabDnsUpstreamTransport,
    initialUpstreamHost: String = DEFAULT_UPSTREAM_HOST,
    initialUpstreamPort: Int = DNS_PORT,
) : LabTrafficObserver {
    private val lock = Any()
    private var policy: Policy = initialPolicy
    private var forwardingMode: LabDnsForwardingMode = initialForwardingMode
    private var upstreamTransport: LabDnsUpstreamTransport = initialUpstreamTransport
    private var upstreamHost: String = initialUpstreamHost
    private var upstreamPort: Int = initialUpstreamPort
    private var captureMode: LabCaptureMode = LabCaptureMode.FULL_TUNNEL_LAB
    private var stats = LabTrafficObservationStats(
        activeModeLabel = captureMode.displayLabel,
        dnsUpstreamHost = upstreamHost,
        dnsUpstreamPort = upstreamPort,
    )

    override fun reset(startedAtMillis: Long) {
        synchronized(lock) {
            stats = LabTrafficObservationStats(
                activeModeLabel = captureMode.displayLabel,
                dnsUpstreamHost = upstreamHost,
                dnsUpstreamPort = upstreamPort,
                lastPacketSummary = "Lab capture started at $startedAtMillis",
            )
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
                lastPacketSummary = "DNS response write failed at $observedAtMillis",
            )
            stats
        }
    }

    override fun markDnsResponseWriteSuccess(observedAtMillis: Long): LabTrafficObservationStats {
        return synchronized(lock) {
            stats = stats.copy(
                dnsResponseWriteSuccessCount = stats.dnsResponseWriteSuccessCount + 1,
                lastPacketSummary = "DNS response written to TUN at $observedAtMillis",
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

    fun updatePolicy(policy: Policy) {
        synchronized(lock) {
            this.policy = policy
            stats = stats.copy(lastPacketSummary = "Lab DNS policy updated")
        }
    }

    fun useDefaultPolicy() {
        updatePolicy(defaultLabPolicy())
    }

    fun configureUpstream(
        forwardingMode: LabDnsForwardingMode,
        upstreamTransport: LabDnsUpstreamTransport,
        upstreamHost: String = DEFAULT_UPSTREAM_HOST,
        upstreamPort: Int = DNS_PORT,
        captureMode: LabCaptureMode = this.captureMode,
    ) {
        synchronized(lock) {
            this.forwardingMode = forwardingMode
            this.upstreamTransport = upstreamTransport
            this.upstreamHost = upstreamHost
            this.upstreamPort = upstreamPort
            this.captureMode = captureMode
            stats = stats.copy(
                activeModeLabel = captureMode.displayLabel,
                dnsUpstreamHost = upstreamHost,
                dnsUpstreamPort = upstreamPort,
                lastPacketSummary = "${captureMode.displayLabel} DNS upstream set to $upstreamHost:$upstreamPort",
            )
        }
    }

    private fun observeDnsIfPresent(
        currentStats: LabTrafficObservationStats,
        parseResult: PacketParseResult,
        originalPacket: ByteArray,
        originalLength: Int,
        observedAtMillis: Long,
    ): LabPacketHandlingResult {
        val udpPayload = parseResult.udpPayload ?: return dropNonDns(currentStats, parseResult.metadata.summary())
        val sourcePort = parseResult.metadata.sourcePort
        val destinationPort = parseResult.metadata.destinationPort
        if (sourcePort != DNS_PORT && destinationPort != DNS_PORT) {
            return dropNonDns(currentStats, "Non-DNS packet dropped")
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
                    hardBypassDecision(question.domain) ?: run {
                        val decision = domainTrafficEvaluator.evaluateDomain(question.domain, policy)
                        LabDnsDomainDecision(
                            domain = question.domain,
                            actionLabel = decision.action.name,
                            reasonLabel = decision.evaluation.reason.name,
                            shouldCreateEvent = decision.evaluation.shouldCreateEvent,
                        )
                    }
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
                val encryptedDnsBlockedCount = decisions.count { it.categoryLabel == ENCRYPTED_DNS_CATEGORY_LABEL }
                val observedStats = currentStats.copy(
                    dnsPacketCount = currentStats.dnsPacketCount + 1,
                    dnsQueryCount = currentStats.dnsQueryCount + dnsResult.questions.size,
                    allowedDomainCount = currentStats.allowedDomainCount + allowedCount,
                    blockedDomainCount = currentStats.blockedDomainCount + blockedCount,
                    alertOnlyDomainCount = currentStats.alertOnlyDomainCount + alertOnlyCount,
                    encryptedDnsBlockedCount = currentStats.encryptedDnsBlockedCount + encryptedDnsBlockedCount,
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
                        dnsOnlyBlockedResponseCount = observedStats.dnsOnlyBlockedResponseCount +
                            if (response.built && captureMode == LabCaptureMode.DNS_ONLY_LAB) 1 else 0,
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
                } else if (allowedCount > 0 && forwardingMode == LabDnsForwardingMode.LAB_UPSTREAM) {
                    forwardAllowedDns(
                        observedStats = observedStats,
                        udpDnsPayload = udpPayload.payload,
                        originalPacket = originalPacket,
                        originalLength = originalLength,
                        allowedCount = allowedCount,
                        alertOnlyCount = alertOnlyCount,
                    )
                } else {
                    stats = observedStats.copy(
                        dnsAllowedDroppedCount = observedStats.dnsAllowedDroppedCount + allowedCount,
                        dnsAlertDroppedCount = observedStats.dnsAlertDroppedCount + alertOnlyCount,
                    )
                    val reason = when {
                        allowedCount > 0 -> "Allowed DNS forwarding disabled in lab mode; packet dropped"
                        alertOnlyCount > 0 -> "Alert-only DNS is not forwarded in lab mode; packet dropped"
                        else -> "DNS query had no actionable decision; packet dropped"
                    }
                    drop(stats, reason)
                }
            }
        }
    }

    private fun forwardAllowedDns(
        observedStats: LabTrafficObservationStats,
        udpDnsPayload: ByteArray,
        originalPacket: ByteArray,
        originalLength: Int,
        allowedCount: Int,
        alertOnlyCount: Int,
    ): LabPacketHandlingResult {
        val upstreamResult = upstreamTransport.query(
            LabDnsUpstreamQuery(
                dnsPayload = udpDnsPayload.copyOf(),
                upstreamHost = upstreamHost,
                upstreamPort = upstreamPort,
            ),
        )
        if (!upstreamResult.success || upstreamResult.responsePayload == null) {
            stats = observedStats.copy(
                dnsAllowedForwardFailureCount = observedStats.dnsAllowedForwardFailureCount + allowedCount,
                dnsOnlyAllowedForwardFailureCount = observedStats.dnsOnlyAllowedForwardFailureCount +
                    if (captureMode == LabCaptureMode.DNS_ONLY_LAB) allowedCount else 0,
                dnsAllowedForwardTimeoutCount = observedStats.dnsAllowedForwardTimeoutCount +
                    if (upstreamResult.reason.contains("timeout", ignoreCase = true)) allowedCount else 0,
                dnsAlertDroppedCount = observedStats.dnsAlertDroppedCount + alertOnlyCount,
                lastPacketSummary = "Allowed DNS upstream failed: ${upstreamResult.reason}",
            )
            return drop(stats, "Allowed DNS upstream failed; packet dropped: ${upstreamResult.reason}")
        }

        val response = dnsUpstreamResponseWrapper.buildResponseFromUpstreamPayload(
            originalPacket = originalPacket,
            originalLength = originalLength,
            upstreamDnsPayload = upstreamResult.responsePayload,
        )
        if (!response.built || response.responseBytes == null) {
            stats = observedStats.copy(
                dnsAllowedForwardFailureCount = observedStats.dnsAllowedForwardFailureCount + allowedCount,
                dnsOnlyAllowedForwardFailureCount = observedStats.dnsOnlyAllowedForwardFailureCount +
                    if (captureMode == LabCaptureMode.DNS_ONLY_LAB) allowedCount else 0,
                dnsAlertDroppedCount = observedStats.dnsAlertDroppedCount + alertOnlyCount,
                lastPacketSummary = "Allowed DNS upstream response could not be wrapped: ${response.reason}",
            )
            return drop(stats, "Allowed DNS upstream response dropped; ${response.reason}")
        }

        stats = observedStats.copy(
            dnsAllowedForwardedCount = observedStats.dnsAllowedForwardedCount + allowedCount,
            dnsOnlyAllowedForwardedCount = observedStats.dnsOnlyAllowedForwardedCount +
                if (captureMode == LabCaptureMode.DNS_ONLY_LAB) allowedCount else 0,
            dnsAlertDroppedCount = observedStats.dnsAlertDroppedCount + alertOnlyCount,
            lastPacketSummary = "Allowed DNS upstream response planned for TUN",
        )
        return LabPacketHandlingResult(
            action = LabPacketAction.WRITE_DNS_UPSTREAM_RESPONSE,
            responseBytes = response.responseBytes,
            stats = stats,
            decisionSummary = "Allowed DNS forwarded to lab upstream",
        )
    }

    private fun hardBypassDecision(domain: DomainName): LabDnsDomainDecision? {
        val match = encryptedDnsResolverSeedList.classify(domain) ?: return null
        return LabDnsDomainDecision(
            domain = domain,
            actionLabel = TrafficAction.BLOCK.name,
            reasonLabel = "ENCRYPTED_DNS_RESOLVER_BLOCKED_IN_LAB",
            shouldCreateEvent = true,
            categoryLabel = match.classification.name,
        )
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

    private fun dropNonDns(
        currentStats: LabTrafficObservationStats,
        reason: String,
    ): LabPacketHandlingResult {
        val nextStats = if (captureMode == LabCaptureMode.DNS_ONLY_LAB) {
            currentStats.copy(
                dnsOnlyUnexpectedNonDnsCount = currentStats.dnsOnlyUnexpectedNonDnsCount + 1,
                lastPacketSummary = "Unexpected non-DNS packet in DNS-only lab: $reason",
            )
        } else {
            currentStats
        }
        return drop(nextStats, reason)
    }

    private fun LabTrafficObservationStats.recordPacket(parseResult: PacketParseResult): LabTrafficObservationStats {
        return copy(
            packetCount = packetCount + 1,
            byteCount = byteCount + parseResult.metadata.byteLength,
            fullTunnelLabPacketCount = fullTunnelLabPacketCount +
                if (captureMode == LabCaptureMode.FULL_TUNNEL_LAB) 1 else 0,
            dnsOnlyLabPacketCount = dnsOnlyLabPacketCount +
                if (captureMode == LabCaptureMode.DNS_ONLY_LAB) 1 else 0,
            malformedPacketCount = malformedPacketCount + if (parseResult.metadata.malformed) 1 else 0,
            lastPacketSummary = parseResult.metadata.summary(),
        )
    }

    companion object {
        const val DNS_PORT = 53
        const val DEFAULT_UPSTREAM_HOST = "1.1.1.1"
        const val ENCRYPTED_DNS_CATEGORY_LABEL = "DNS_OVER_HTTPS"
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

object NoConfiguredLabDnsUpstreamTransport : LabDnsUpstreamTransport {
    override fun query(query: LabDnsUpstreamQuery): LabDnsUpstreamResult {
        return LabDnsUpstreamResult.failure("Lab upstream transport is not configured")
    }
}
