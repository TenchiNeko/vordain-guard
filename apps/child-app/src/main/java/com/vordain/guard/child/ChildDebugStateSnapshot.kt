package com.vordain.guard.child

data class ChildDebugStateSnapshot(
    val childDeviceId: String = DEFAULT_CHILD_DEVICE_ID,
    val childDisplayName: String = DEFAULT_CHILD_DISPLAY_NAME,
    val childFingerprint: String = DEFAULT_CHILD_FINGERPRINT,
    val latestPolicyPayload: String? = null,
    val latestPolicyAppliedAtMillis: Long = 0L,
    val latestPolicyVersion: String? = null,
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
) {
    companion object {
        const val DEFAULT_CHILD_DEVICE_ID = "child-debug-device"
        const val DEFAULT_CHILD_DISPLAY_NAME = "Child Debug Tablet"
        const val DEFAULT_CHILD_FINGERPRINT = "debug-child-fingerprint"
    }
}
