package com.vordain.guard.vpn.lab

import com.vordain.guard.core.model.DomainName
import com.vordain.guard.vpn.packet.ParsedPacketMetadata

data class LabDnsDomainDecision(
    val domain: DomainName,
    val actionLabel: String,
    val reasonLabel: String,
    val shouldCreateEvent: Boolean,
    val categoryLabel: String? = null,
)

data class LabDnsObservation(
    val observedAtMillis: Long,
    val queriedDomains: List<DomainName>,
    val packetMetadata: ParsedPacketMetadata,
    val decisions: List<LabDnsDomainDecision>,
) {
    fun summary(): String {
        return if (decisions.isEmpty()) {
            "DNS query with no evaluated domains"
        } else {
            decisions.joinToString(separator = "\n") { decision ->
                "${decision.domain.value}: ${decision.actionLabel} (${decision.reasonLabel})"
            }
        }
    }
}

data class LabTrafficObservationStats(
    val activeModeLabel: String = LabCaptureMode.FULL_TUNNEL_LAB.displayLabel,
    val packetCount: Long = 0,
    val byteCount: Long = 0,
    val fullTunnelLabPacketCount: Long = 0,
    val dnsOnlyLabPacketCount: Long = 0,
    val dnsOnlyUnexpectedNonDnsCount: Long = 0,
    val dnsUpstreamHost: String = "1.1.1.1",
    val dnsUpstreamPort: Int = 53,
    val dnsPacketCount: Long = 0,
    val dnsQueryCount: Long = 0,
    val allowedDomainCount: Long = 0,
    val blockedDomainCount: Long = 0,
    val alertOnlyDomainCount: Long = 0,
    val dnsBlockedResponseCount: Long = 0,
    val dnsOnlyBlockedResponseCount: Long = 0,
    val dnsAllowedDroppedCount: Long = 0,
    val dnsAllowedForwardedCount: Long = 0,
    val dnsOnlyAllowedForwardedCount: Long = 0,
    val dnsAllowedForwardFailureCount: Long = 0,
    val dnsOnlyAllowedForwardFailureCount: Long = 0,
    val dnsAllowedForwardTimeoutCount: Long = 0,
    val dnsAlertDroppedCount: Long = 0,
    val dnsResponseWriteSuccessCount: Long = 0,
    val dnsResponseWriteFailureCount: Long = 0,
    val encryptedDnsBlockedCount: Long = 0,
    val malformedPacketCount: Long = 0,
    val malformedDnsCount: Long = 0,
    val lastPacketSummary: String? = null,
    val lastDnsObservation: LabDnsObservation? = null,
    val recentDnsObservations: List<LabDnsObservation> = emptyList(),
)

enum class LabPacketAction {
    DROP,
    WRITE_DNS_BLOCK_RESPONSE,
    WRITE_DNS_UPSTREAM_RESPONSE,
    IGNORE,
}

enum class LabCaptureMode(val displayLabel: String) {
    FULL_TUNNEL_LAB("Full-tunnel lab"),
    DNS_ONLY_LAB("DNS-only lab"),
}

data class LabPacketHandlingResult(
    val action: LabPacketAction,
    val responseBytes: ByteArray?,
    val stats: LabTrafficObservationStats,
    val decisionSummary: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LabPacketHandlingResult) return false
        return action == other.action &&
            responseBytes.contentEquals(other.responseBytes) &&
            stats == other.stats &&
            decisionSummary == other.decisionSummary
    }

    override fun hashCode(): Int {
        var result = action.hashCode()
        result = 31 * result + (responseBytes?.contentHashCode() ?: 0)
        result = 31 * result + stats.hashCode()
        result = 31 * result + (decisionSummary?.hashCode() ?: 0)
        return result
    }
}

class LabDnsUpstreamQuery(
    val dnsPayload: ByteArray,
    val upstreamHost: String,
    val upstreamPort: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LabDnsUpstreamQuery) return false
        return dnsPayload.contentEquals(other.dnsPayload) &&
            upstreamHost == other.upstreamHost &&
            upstreamPort == other.upstreamPort
    }

    override fun hashCode(): Int {
        var result = dnsPayload.contentHashCode()
        result = 31 * result + upstreamHost.hashCode()
        result = 31 * result + upstreamPort
        return result
    }
}

class LabDnsUpstreamResult(
    val success: Boolean,
    val responsePayload: ByteArray?,
    val reason: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LabDnsUpstreamResult) return false
        return success == other.success &&
            responsePayload.contentEquals(other.responsePayload) &&
            reason == other.reason
    }

    override fun hashCode(): Int {
        var result = success.hashCode()
        result = 31 * result + (responsePayload?.contentHashCode() ?: 0)
        result = 31 * result + reason.hashCode()
        return result
    }

    companion object {
        fun success(responsePayload: ByteArray): LabDnsUpstreamResult {
            return LabDnsUpstreamResult(
                success = true,
                responsePayload = responsePayload,
                reason = "Upstream DNS response received",
            )
        }

        fun failure(reason: String): LabDnsUpstreamResult {
            return LabDnsUpstreamResult(success = false, responsePayload = null, reason = reason)
        }
    }
}

interface LabDnsUpstreamTransport {
    fun query(query: LabDnsUpstreamQuery): LabDnsUpstreamResult
}

enum class LabDnsForwardingMode {
    DISABLED,
    LAB_UPSTREAM,
}
