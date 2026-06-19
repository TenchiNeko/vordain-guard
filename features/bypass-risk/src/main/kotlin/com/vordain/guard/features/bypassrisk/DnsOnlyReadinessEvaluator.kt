package com.vordain.guard.features.bypassrisk

enum class DnsOnlyReadinessStatus {
    NOT_READY,
    READY_FOR_DNS_LAB,
    NEEDS_REVIEW,
    HIGH_RISK,
    UNKNOWN,
}

data class DnsOnlyReadinessInput(
    val vpnPermissionConfirmed: Boolean,
    val alwaysOnVpnConfirmed: Boolean,
    val blockWithoutVpnConfirmed: Boolean,
    val settingsLockConfirmed: Boolean,
    val pinCompromiseSuspected: Boolean,
    val activePolicyVersion: String?,
    val dnsOnlyLabAvailable: Boolean,
    val localLabOnlyMode: Boolean,
    val bypassRiskSummary: BypassRiskSummary,
)

data class DnsOnlyReadinessResult(
    val status: DnsOnlyReadinessStatus,
    val reason: String,
)

class DnsOnlyReadinessEvaluator {
    fun evaluate(input: DnsOnlyReadinessInput): DnsOnlyReadinessResult {
        if (!input.dnsOnlyLabAvailable) {
            return DnsOnlyReadinessResult(DnsOnlyReadinessStatus.NOT_READY, "DNS-only lab mode is not available.")
        }
        if (input.pinCompromiseSuspected) {
            return DnsOnlyReadinessResult(DnsOnlyReadinessStatus.HIGH_RISK, "Parent PIN compromise is suspected.")
        }
        if (input.bypassRiskSummary.overallStatus == BypassRiskOverallStatus.HIGH_RISK) {
            return DnsOnlyReadinessResult(DnsOnlyReadinessStatus.HIGH_RISK, "A high bypass risk was found.")
        }
        if (input.bypassRiskSummary.overallStatus != BypassRiskOverallStatus.READY_FOR_DNS_LAB) {
            return DnsOnlyReadinessResult(DnsOnlyReadinessStatus.NEEDS_REVIEW, "DNS-only bypass risks still need review.")
        }
        if (!input.vpnPermissionConfirmed) {
            return DnsOnlyReadinessResult(DnsOnlyReadinessStatus.NEEDS_REVIEW, "VPN permission still needs confirmation.")
        }
        if (!input.alwaysOnVpnConfirmed || !input.blockWithoutVpnConfirmed) {
            val reason = if (input.localLabOnlyMode) {
                "Local lab can run, but Always-on VPN and Block without VPN still need review."
            } else {
                "Always-on VPN and Block without VPN are required for the full hardening path."
            }
            return DnsOnlyReadinessResult(DnsOnlyReadinessStatus.NEEDS_REVIEW, reason)
        }
        if (!input.settingsLockConfirmed) {
            return DnsOnlyReadinessResult(DnsOnlyReadinessStatus.NEEDS_REVIEW, "Settings/App Lock still needs parent confirmation.")
        }
        if (input.activePolicyVersion.isNullOrBlank()) {
            return DnsOnlyReadinessResult(DnsOnlyReadinessStatus.NEEDS_REVIEW, "A verified debug policy is not active.")
        }
        return DnsOnlyReadinessResult(DnsOnlyReadinessStatus.READY_FOR_DNS_LAB, "Ready for DNS-only lab test.")
    }
}
