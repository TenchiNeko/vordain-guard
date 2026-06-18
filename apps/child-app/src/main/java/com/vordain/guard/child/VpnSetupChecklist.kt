package com.vordain.guard.child

data class VpnSetupChecklist(
    val vpnPermission: SetupCheckState,
    val alwaysOnVpn: SetupCheckState,
    val blockConnectionsWithoutVpn: SetupCheckState,
    val batteryOptimizationWarning: SetupCheckState,
    val appProtection: SetupCheckState,
) {
    val isComplete: Boolean
        get() = checks.all { check -> check == SetupCheckState.CONFIGURED || check == SetupCheckState.USER_CONFIRMED }

    val hasUnknownChecks: Boolean
        get() = checks.any { check -> check == SetupCheckState.UNKNOWN }

    private val checks: List<SetupCheckState>
        get() = listOf(
            vpnPermission,
            alwaysOnVpn,
            blockConnectionsWithoutVpn,
            batteryOptimizationWarning,
            appProtection,
        )
}
