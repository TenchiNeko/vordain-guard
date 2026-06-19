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
        lines += "Debug policy version: ${state.currentPolicyVersion}"
        state.policyHandoffResult?.let { result ->
            lines += "Policy handoff: ${result.reason} / ${result.policyVersion?.value ?: "unchanged"}"
        }
        state.compatibilityResult?.let { result ->
            lines += "Compatibility demo: ${result.mode} / ${result.action} / ${result.reason}"
        }
        state.reviewResult?.let { result ->
            lines += "Review demo domain: ${result.sanitizedDomain ?: "none"}"
        }
        lines += "Lab capture status:"
        lines += "Packets: ${state.labCaptureStats.packetCount}"
        lines += "Bytes: ${state.labCaptureStats.byteCount}"
        lines += "IPv4/IPv6: ${state.labCaptureStats.ipv4Count}/${state.labCaptureStats.ipv6Count}"
        lines += "TCP/UDP/ICMP: ${state.labCaptureStats.tcpCount}/${state.labCaptureStats.udpCount}/${state.labCaptureStats.icmpCount}"
        lines += "Malformed: ${state.labCaptureStats.malformedCount}"
        lines += "Last packet: ${state.labCaptureStats.lastPacketSummary ?: "none"}"
        lines += ChildVpnSmokeLabels.LAB_LOCAL_ONLY
        lines += ChildVpnSmokeLabels.LAB_NOT_FULL_PROTECTION
        lines += "Local debug events: ${state.localEvents.size}"
        lines += "Filtering enabled: no"
        return lines.joinToString(separator = "\n")
    }
}
