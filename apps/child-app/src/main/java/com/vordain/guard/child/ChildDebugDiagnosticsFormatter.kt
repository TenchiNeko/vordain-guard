package com.vordain.guard.child

class ChildDebugDiagnosticsFormatter {
    fun format(
        diagnostics: ChildVpnSmokeDiagnostics,
        state: ChildDebugDashboardState,
    ): String {
        val lines = mutableListOf<String>()
        lines += diagnostics.asClipboardText()
        lines += "Setup checklist:"
        lines += "VPN permission: ${state.setupChecklist.vpnPermission}"
        lines += "Start shell: ${state.shellStatus}"
        lines += "Always-on VPN: ${state.setupChecklist.alwaysOnVpn}"
        lines += "Block connections without VPN: ${state.setupChecklist.blockConnectionsWithoutVpn}"
        lines += "Battery optimization: ${state.setupChecklist.batteryOptimizationWarning}"
        lines += "App protection: ${state.setupChecklist.appProtection}"
        state.policyResult?.let { result ->
            lines += "Policy demo: ${result.decision} / ${result.reason} / ${result.normalizedDomain ?: "none"}"
        }
        state.compatibilityResult?.let { result ->
            lines += "Compatibility demo: ${result.mode} / ${result.action} / ${result.reason}"
        }
        state.reviewResult?.let { result ->
            lines += "Review demo domain: ${result.sanitizedDomain ?: "none"}"
        }
        lines += "Local debug events: ${state.localEvents.size}"
        lines += "Filtering enabled: no"
        return lines.joinToString(separator = "\n")
    }
}
