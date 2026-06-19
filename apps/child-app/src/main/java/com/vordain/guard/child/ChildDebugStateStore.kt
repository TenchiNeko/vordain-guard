package com.vordain.guard.child

import android.content.Context

class ChildDebugStateStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): ChildDebugStateSnapshot {
        return ChildDebugStateSnapshot(
            childDeviceId = preferences.getString(KEY_CHILD_DEVICE_ID, null)
                ?: ChildDebugStateSnapshot.DEFAULT_CHILD_DEVICE_ID,
            latestPolicyPayload = preferences.getString(KEY_LATEST_POLICY_PAYLOAD, null),
            latestPolicyVersion = preferences.getString(KEY_LATEST_POLICY_VERSION, null),
            latestAllowDomainsCsv = preferences.getString(KEY_LATEST_ALLOW_DOMAINS_CSV, null),
            latestBlockDomainsCsv = preferences.getString(KEY_LATEST_BLOCK_DOMAINS_CSV, null),
            vpnPermissionStatusLabel = preferences.getString(KEY_VPN_PERMISSION_STATUS_LABEL, null)
                ?: ChildVpnSmokeLabels.PERMISSION_UNKNOWN,
            lastVpnCommandLabel = preferences.getString(KEY_LAST_VPN_COMMAND_LABEL, null)
                ?: ChildVpnSmokeLabels.COMMAND_NONE,
            shellStatusLabel = preferences.getString(KEY_SHELL_STATUS_LABEL, null)
                ?: ChildVpnSmokeLabels.STATUS_NOT_RUNNING,
            setupVpnPermissionStatus = preferences.getString(KEY_SETUP_VPN_PERMISSION_STATUS, null)
                ?: SetupCheckState.UNKNOWN.name,
            setupStartShellStatus = preferences.getString(KEY_SETUP_START_SHELL_STATUS, null)
                ?: SetupCheckState.UNKNOWN.name,
            setupForegroundNotificationStatus = preferences.getString(KEY_SETUP_FOREGROUND_NOTIFICATION_STATUS, null)
                ?: SetupCheckState.UNKNOWN.name,
            setupAlwaysOnVpnStatus = preferences.getString(KEY_SETUP_ALWAYS_ON_VPN_STATUS, null)
                ?: SetupCheckState.UNKNOWN.name,
            setupBlockWithoutVpnStatus = preferences.getString(KEY_SETUP_BLOCK_WITHOUT_VPN_STATUS, null)
                ?: SetupCheckState.UNKNOWN.name,
            setupBatteryOptimizationStatus = preferences.getString(KEY_SETUP_BATTERY_OPTIMIZATION_STATUS, null)
                ?: SetupCheckState.UNKNOWN.name,
            lastDiagnosticsText = preferences.getString(KEY_LAST_DIAGNOSTICS_TEXT, null),
        )
    }

    fun save(snapshot: ChildDebugStateSnapshot) {
        preferences.edit()
            .putString(KEY_CHILD_DEVICE_ID, snapshot.childDeviceId)
            .putString(KEY_LATEST_POLICY_PAYLOAD, snapshot.latestPolicyPayload)
            .putString(KEY_LATEST_POLICY_VERSION, snapshot.latestPolicyVersion)
            .putString(KEY_LATEST_ALLOW_DOMAINS_CSV, snapshot.latestAllowDomainsCsv)
            .putString(KEY_LATEST_BLOCK_DOMAINS_CSV, snapshot.latestBlockDomainsCsv)
            .putString(KEY_VPN_PERMISSION_STATUS_LABEL, snapshot.vpnPermissionStatusLabel)
            .putString(KEY_LAST_VPN_COMMAND_LABEL, snapshot.lastVpnCommandLabel)
            .putString(KEY_SHELL_STATUS_LABEL, snapshot.shellStatusLabel)
            .putString(KEY_SETUP_VPN_PERMISSION_STATUS, snapshot.setupVpnPermissionStatus)
            .putString(KEY_SETUP_START_SHELL_STATUS, snapshot.setupStartShellStatus)
            .putString(KEY_SETUP_FOREGROUND_NOTIFICATION_STATUS, snapshot.setupForegroundNotificationStatus)
            .putString(KEY_SETUP_ALWAYS_ON_VPN_STATUS, snapshot.setupAlwaysOnVpnStatus)
            .putString(KEY_SETUP_BLOCK_WITHOUT_VPN_STATUS, snapshot.setupBlockWithoutVpnStatus)
            .putString(KEY_SETUP_BATTERY_OPTIMIZATION_STATUS, snapshot.setupBatteryOptimizationStatus)
            .putString(KEY_LAST_DIAGNOSTICS_TEXT, snapshot.lastDiagnosticsText)
            .apply()
    }

    companion object {
        const val PREFERENCES_NAME = "vordain_child_debug_state"
        const val KEY_CHILD_DEVICE_ID = "child_device_id"
        const val KEY_LATEST_POLICY_PAYLOAD = "latest_policy_payload"
        const val KEY_LATEST_POLICY_VERSION = "latest_policy_version"
        const val KEY_LATEST_ALLOW_DOMAINS_CSV = "latest_allow_domains_csv"
        const val KEY_LATEST_BLOCK_DOMAINS_CSV = "latest_block_domains_csv"
        const val KEY_VPN_PERMISSION_STATUS_LABEL = "vpn_permission_status_label"
        const val KEY_LAST_VPN_COMMAND_LABEL = "last_vpn_command_label"
        const val KEY_SHELL_STATUS_LABEL = "shell_status_label"
        const val KEY_SETUP_VPN_PERMISSION_STATUS = "setup_vpn_permission_status"
        const val KEY_SETUP_START_SHELL_STATUS = "setup_start_shell_status"
        const val KEY_SETUP_FOREGROUND_NOTIFICATION_STATUS = "setup_foreground_notification_status"
        const val KEY_SETUP_ALWAYS_ON_VPN_STATUS = "setup_always_on_vpn_status"
        const val KEY_SETUP_BLOCK_WITHOUT_VPN_STATUS = "setup_block_without_vpn_status"
        const val KEY_SETUP_BATTERY_OPTIMIZATION_STATUS = "setup_battery_optimization_status"
        const val KEY_LAST_DIAGNOSTICS_TEXT = "last_diagnostics_text"

        val persistedKeys = setOf(
            KEY_CHILD_DEVICE_ID,
            KEY_LATEST_POLICY_PAYLOAD,
            KEY_LATEST_POLICY_VERSION,
            KEY_LATEST_ALLOW_DOMAINS_CSV,
            KEY_LATEST_BLOCK_DOMAINS_CSV,
            KEY_VPN_PERMISSION_STATUS_LABEL,
            KEY_LAST_VPN_COMMAND_LABEL,
            KEY_SHELL_STATUS_LABEL,
            KEY_SETUP_VPN_PERMISSION_STATUS,
            KEY_SETUP_START_SHELL_STATUS,
            KEY_SETUP_FOREGROUND_NOTIFICATION_STATUS,
            KEY_SETUP_ALWAYS_ON_VPN_STATUS,
            KEY_SETUP_BLOCK_WITHOUT_VPN_STATUS,
            KEY_SETUP_BATTERY_OPTIMIZATION_STATUS,
            KEY_LAST_DIAGNOSTICS_TEXT,
        )
    }
}
