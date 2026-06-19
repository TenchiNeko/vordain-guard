package com.vordain.guard.core.statusreport

class ChildSecurityStatusEvaluator {
    fun evaluate(input: ChildSecurityStatusInput): ChildSecurityStatusReport {
        val signals = buildSet {
            if (input.vpnPermissionConfirmed) add(ChildSecuritySignal.VPN_PERMISSION_CONFIRMED)
            if (input.alwaysOnVpnConfirmed) add(ChildSecuritySignal.VPN_ALWAYS_ON_CONFIRMED)
            if (input.blockWithoutVpnConfirmed) add(ChildSecuritySignal.BLOCK_WITHOUT_VPN_CONFIRMED)
            if (input.settingsLockConfirmed) add(ChildSecuritySignal.SETTINGS_LOCK_CONFIRMED)
            if (input.developerOptionsDisabledConfirmed) add(ChildSecuritySignal.DEVELOPER_OPTIONS_DISABLED_CONFIRMED)
            if (input.adbDisabledConfirmed) add(ChildSecuritySignal.ADB_DISABLED_CONFIRMED)
            if (input.noUnrestrictedProfilesConfirmed) add(ChildSecuritySignal.NO_UNRESTRICTED_PROFILES_CONFIRMED)
            if (input.policyApplied) add(ChildSecuritySignal.POLICY_APPLIED)
            if (input.heartbeatFresh) add(ChildSecuritySignal.HEARTBEAT_FRESH)
            if (input.vpnSessionRunning) add(ChildSecuritySignal.VPN_SESSION_RUNNING)
            if (input.labCaptureActive) add(ChildSecuritySignal.LAB_CAPTURE_ACTIVE)
            if (input.pinCompromiseSuspected) add(ChildSecuritySignal.PIN_COMPROMISE_SUSPECTED)
            if (input.vpnStopped) add(ChildSecuritySignal.VPN_STOPPED)
        }
        return ChildSecurityStatusReport(
            childDeviceId = input.childDeviceId,
            generatedAtMillis = input.generatedAtMillis,
            overallStatus = determineStatus(input, signals),
            signals = signals,
            policyVersion = input.policyVersion?.takeIf(String::isNotBlank),
            vpnSessionLabel = input.vpnSessionLabel?.takeIf(String::isNotBlank),
            heartbeatLabel = input.heartbeatLabel?.takeIf(String::isNotBlank),
            setupSummaryLabel = input.setupSummaryLabel?.takeIf(String::isNotBlank),
            bypassRiskLabel = input.bypassRiskLabel?.takeIf(String::isNotBlank),
            activeMode = input.activeMode,
            dnsBlockedResponseCount = input.dnsBlockedResponseCount,
            dnsAllowedForwardedCount = input.dnsAllowedForwardedCount,
            dnsAllowedForwardFailureCount = input.dnsAllowedForwardFailureCount,
        )
    }

    private fun determineStatus(
        input: ChildSecurityStatusInput,
        signals: Set<ChildSecuritySignal>,
    ): ChildSecurityOverallStatus {
        if (input.pinCompromiseSuspected) {
            return ChildSecurityOverallStatus.PIN_COMPROMISE_SUSPECTED
        }
        if (input.vpnStopped) {
            return ChildSecurityOverallStatus.VPN_STOPPED
        }
        val ready = input.vpnPermissionConfirmed &&
            input.alwaysOnVpnConfirmed &&
            input.blockWithoutVpnConfirmed &&
            input.settingsLockConfirmed
        if (ready) {
            return ChildSecurityOverallStatus.READY_FOR_LAB_TEST
        }
        if (signals.isEmpty()) {
            return ChildSecurityOverallStatus.NOT_STARTED
        }
        if (!input.alwaysOnVpnConfirmed || !input.blockWithoutVpnConfirmed || !input.settingsLockConfirmed) {
            return ChildSecurityOverallStatus.NEEDS_ATTENTION
        }
        return ChildSecurityOverallStatus.SETUP_IN_PROGRESS
    }
}
