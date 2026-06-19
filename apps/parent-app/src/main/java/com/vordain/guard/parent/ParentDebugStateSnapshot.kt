package com.vordain.guard.parent

data class ParentDebugStateSnapshot(
    val targetChildDeviceId: String = DEFAULT_TARGET_CHILD_DEVICE_ID,
    val policyVersion: String = DEFAULT_POLICY_VERSION,
    val allowDomainsText: String = DEFAULT_ALLOW_DOMAINS,
    val blockDomainsText: String = DEFAULT_BLOCK_DOMAINS,
    val blockKnownProxyDomains: Boolean = true,
    val blockEncryptedDnsResolvers: Boolean = true,
    val blockUnknownDomains: Boolean = false,
    val selectedPolicyPreset: String = DEFAULT_POLICY_PRESET,
    val latestGeneratedPayload: String? = null,
    val pairingSessionId: String = DEFAULT_PAIRING_SESSION_ID,
    val parentDeviceId: String = DEFAULT_PARENT_DEVICE_ID,
    val parentDisplayName: String = DEFAULT_PARENT_DISPLAY_NAME,
    val parentFingerprint: String = DEFAULT_PARENT_FINGERPRINT,
    val verificationCode: String = DEFAULT_VERIFICATION_CODE,
    val latestPairingInvitePayload: String? = null,
    val latestPairingAcceptancePayload: String? = null,
    val acceptedChildSummary: String? = null,
    val latestHardeningSetupReportPayload: String? = null,
    val latestChildSecurityStatusReportPayload: String? = null,
    val latestBypassRiskReportPayload: String? = null,
) {
    companion object {
        const val DEFAULT_TARGET_CHILD_DEVICE_ID = "child-debug-device"
        const val DEFAULT_POLICY_VERSION = "debug-1"
        const val DEFAULT_ALLOW_DOMAINS = "school.edu"
        const val DEFAULT_BLOCK_DOMAINS = "proxy.example"
        const val DEFAULT_POLICY_PRESET = "BASIC_DNS_GUARD"
        const val DEFAULT_PAIRING_SESSION_ID = "debug-session-1"
        const val DEFAULT_PARENT_DEVICE_ID = "parent-debug-device"
        const val DEFAULT_PARENT_DISPLAY_NAME = "Parent Debug Device"
        const val DEFAULT_PARENT_FINGERPRINT = "debug-parent-fingerprint"
        const val DEFAULT_VERIFICATION_CODE = "123456"
    }
}
