package com.vordain.guard.child

data class ChildDebugStateSnapshot(
    val childDeviceId: String = DEFAULT_CHILD_DEVICE_ID,
    val childDisplayName: String = DEFAULT_CHILD_DISPLAY_NAME,
    val childFingerprint: String = DEFAULT_CHILD_FINGERPRINT,
    val latestPolicyPayload: String? = null,
    val latestPolicyAppliedAtMillis: Long = 0L,
    val latestPolicyVersion: String? = null,
    val latestPolicyPresetName: String? = null,
    val latestPolicyDisplayLabel: String? = null,
    val latestPolicyBlockEncryptedDnsResolvers: Boolean = true,
    val latestAllowDomainsCsv: String? = null,
    val latestBlockDomainsCsv: String? = null,
    val vpnPermissionStatusLabel: String = ChildVpnSmokeLabels.PERMISSION_UNKNOWN,
    val lastVpnCommandLabel: String = ChildVpnSmokeLabels.COMMAND_NONE,
    val shellStatusLabel: String = ChildVpnSmokeLabels.STATUS_NOT_RUNNING,
    val setupVpnPermissionStatus: String = SetupCheckState.UNKNOWN.name,
    val setupStartShellStatus: String = SetupCheckState.UNKNOWN.name,
    val setupForegroundNotificationStatus: String = SetupCheckState.UNKNOWN.name,
    val setupAlwaysOnVpnStatus: String = SetupCheckState.UNKNOWN.name,
    val setupBlockWithoutVpnStatus: String = SetupCheckState.UNKNOWN.name,
    val setupBatteryOptimizationStatus: String = SetupCheckState.UNKNOWN.name,
    val lastDiagnosticsText: String? = null,
    val latestPairingInvitePayload: String? = null,
    val latestPairingAcceptancePayload: String? = null,
    val acceptedParentSummary: String? = null,
    val latestHardeningSetupReportPayload: String? = null,
    val latestChildSecurityStatusReportPayload: String? = null,
    val latestBypassRiskReportPayload: String? = null,
    val latestChildSyncBundlePayload: String? = null,
    val latestParentSyncBundlePayload: String? = null,
    val relayBaseUrl: String = DEFAULT_RELAY_BASE_URL,
    val parentRelayDeviceId: String = DEFAULT_PARENT_RELAY_DEVICE_ID,
    val latestRelayMessageId: String? = null,
    val latestRelayDiagnostics: String? = null,
    val expectedBasicDnsGuardRunning: Boolean = false,
    val latestVpnRuntimeStatus: String = DEFAULT_RUNTIME_STATUS,
    val latestHeartbeatStatus: String = DEFAULT_HEARTBEAT_STATUS,
    val currentOnboardingStep: String = DEFAULT_ONBOARDING_STEP,
    val schemaVersion: Int = SCHEMA_VERSION,
) {
    companion object {
        const val DEFAULT_CHILD_DEVICE_ID = "child-debug-device"
        const val DEFAULT_CHILD_DISPLAY_NAME = "Child Debug Tablet"
        const val DEFAULT_CHILD_FINGERPRINT = "debug-child-fingerprint"
        const val DEFAULT_RELAY_BASE_URL = "http://192.168.68.81:8081"
        const val DEFAULT_PARENT_RELAY_DEVICE_ID = "parent-debug-device"
        const val DEFAULT_RUNTIME_STATUS = "Unknown"
        const val DEFAULT_HEARTBEAT_STATUS = "Heartbeat missing"
        const val DEFAULT_ONBOARDING_STEP = "CONFIRM_CHILD_DEVICE"
        const val SCHEMA_VERSION = 1
    }
}
