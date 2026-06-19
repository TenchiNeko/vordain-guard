package com.vordain.guard.core.statusreport

import com.vordain.guard.core.model.DeviceId

enum class ChildSecurityOverallStatus {
    NOT_STARTED,
    SETUP_IN_PROGRESS,
    READY_FOR_LAB_TEST,
    NEEDS_ATTENTION,
    VPN_STOPPED,
    PIN_COMPROMISE_SUSPECTED,
    UNKNOWN,
}

enum class ChildSecuritySignal {
    VPN_PERMISSION_CONFIRMED,
    VPN_ALWAYS_ON_CONFIRMED,
    BLOCK_WITHOUT_VPN_CONFIRMED,
    SETTINGS_LOCK_CONFIRMED,
    DEVELOPER_OPTIONS_DISABLED_CONFIRMED,
    ADB_DISABLED_CONFIRMED,
    NO_UNRESTRICTED_PROFILES_CONFIRMED,
    POLICY_APPLIED,
    HEARTBEAT_FRESH,
    VPN_SESSION_RUNNING,
    LAB_CAPTURE_ACTIVE,
    PIN_COMPROMISE_SUSPECTED,
    VPN_STOPPED,
}

enum class ChildSecurityActiveMode {
    NONE,
    BASIC_DNS_GUARD,
    DNS_ONLY_LAB,
    FULL_TUNNEL_LAB,
}

data class ChildSecurityStatusReport(
    val childDeviceId: DeviceId,
    val generatedAtMillis: Long,
    val overallStatus: ChildSecurityOverallStatus,
    val signals: Set<ChildSecuritySignal>,
    val policyVersion: String?,
    val vpnSessionLabel: String?,
    val heartbeatLabel: String?,
    val setupSummaryLabel: String?,
    val bypassRiskLabel: String?,
    val activePolicySource: String? = null,
    val activePolicyPreset: String? = null,
    val encryptedDnsBlockingEnabled: Boolean = true,
    val proxyBlockingEnabled: Boolean = true,
    val policyAllowDomainCount: Int = 0,
    val policyBlockDomainCount: Int = 0,
    val activeMode: ChildSecurityActiveMode = ChildSecurityActiveMode.NONE,
    val dnsBlockedResponseCount: Long = 0,
    val dnsAllowedForwardedCount: Long = 0,
    val dnsAllowedForwardFailureCount: Long = 0,
    val warningText: String = WARNING_TEXT,
) {
    companion object {
        const val WARNING_TEXT = "Traffic filtering is not production-enabled yet."
    }
}

data class ChildSecurityStatusInput(
    val childDeviceId: DeviceId,
    val generatedAtMillis: Long,
    val vpnPermissionConfirmed: Boolean,
    val alwaysOnVpnConfirmed: Boolean,
    val blockWithoutVpnConfirmed: Boolean,
    val settingsLockConfirmed: Boolean,
    val developerOptionsDisabledConfirmed: Boolean,
    val adbDisabledConfirmed: Boolean,
    val noUnrestrictedProfilesConfirmed: Boolean,
    val policyVersion: String?,
    val policyApplied: Boolean,
    val vpnSessionLabel: String?,
    val vpnSessionRunning: Boolean,
    val vpnStopped: Boolean,
    val heartbeatLabel: String?,
    val heartbeatFresh: Boolean,
    val setupSummaryLabel: String?,
    val bypassRiskLabel: String?,
    val activePolicySource: String? = null,
    val activePolicyPreset: String? = null,
    val encryptedDnsBlockingEnabled: Boolean = true,
    val proxyBlockingEnabled: Boolean = true,
    val policyAllowDomainCount: Int = 0,
    val policyBlockDomainCount: Int = 0,
    val labCaptureActive: Boolean,
    val pinCompromiseSuspected: Boolean,
    val activeMode: ChildSecurityActiveMode = ChildSecurityActiveMode.NONE,
    val dnsBlockedResponseCount: Long = 0,
    val dnsAllowedForwardedCount: Long = 0,
    val dnsAllowedForwardFailureCount: Long = 0,
)

data class BasicDnsGuardDiagnosticsReport(
    val childDeviceId: DeviceId,
    val generatedAtMillis: Long,
    val mode: ChildSecurityActiveMode,
    val activePolicySource: String,
    val activePolicyVersion: String?,
    val activePreset: String?,
    val readinessStatus: String,
    val hardeningSummary: String,
    val bypassRiskSummary: String,
    val dnsBlockedCount: Long,
    val dnsAllowedForwardedCount: Long,
    val encryptedDnsBlockedCount: Long,
    val dnsFailureCount: Long,
    val warningText: String = WARNING_TEXT,
) {
    companion object {
        const val WARNING_TEXT = "Basic DNS Guard is DNS-only enforcement. Not full protection."
    }
}
