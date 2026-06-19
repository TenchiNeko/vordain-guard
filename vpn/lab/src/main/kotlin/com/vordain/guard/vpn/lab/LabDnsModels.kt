package com.vordain.guard.vpn.lab

import com.vordain.guard.core.model.DomainName
import com.vordain.guard.vpn.packet.ParsedPacketMetadata

data class LabDnsDomainDecision(
    val domain: DomainName,
    val actionLabel: String,
    val reasonLabel: String,
    val shouldCreateEvent: Boolean,
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
    val packetCount: Long = 0,
    val byteCount: Long = 0,
    val dnsPacketCount: Long = 0,
    val dnsQueryCount: Long = 0,
    val allowedDomainCount: Long = 0,
    val blockedDomainCount: Long = 0,
    val alertOnlyDomainCount: Long = 0,
    val malformedPacketCount: Long = 0,
    val malformedDnsCount: Long = 0,
    val lastPacketSummary: String? = null,
    val lastDnsObservation: LabDnsObservation? = null,
    val recentDnsObservations: List<LabDnsObservation> = emptyList(),
)
