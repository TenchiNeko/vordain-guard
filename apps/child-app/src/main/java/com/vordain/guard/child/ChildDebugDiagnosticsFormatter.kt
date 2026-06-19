package com.vordain.guard.child

class ChildDebugDiagnosticsFormatter {
    fun format(
        diagnostics: ChildVpnSmokeDiagnostics,
        state: ChildDebugDashboardState,
    ): String {
        val lines = mutableListOf<String>()
        lines += diagnostics.asClipboardText()
        lines += "Setup checklist:"
        lines += "Hardening summary: ${state.hardeningSetupSnapshot.summaryStatus}"
        state.hardeningSetupSnapshot.items.forEach { item ->
            lines += "${item.step}: ${item.status} / ${item.evidenceType} / ${item.note ?: "no note"}"
        }
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
        lines += "Lab capture aggregate counters:"
        lines += "Packets: ${state.labCaptureStats.packetCount}"
        lines += "Bytes: ${state.labCaptureStats.byteCount}"
        lines += "Lab capture mode: ${state.labCaptureStats.activeModeLabel}"
        lines += "Full-tunnel lab packets: ${state.labCaptureStats.fullTunnelLabPacketCount}"
        lines += "DNS-only lab packets: ${state.labCaptureStats.dnsOnlyLabPacketCount}"
        lines += "DNS-only unexpected non-DNS packets: ${state.labCaptureStats.dnsOnlyUnexpectedNonDnsCount}"
        lines += "DNS packets: ${state.labCaptureStats.dnsPacketCount}"
        lines += "DNS queries: ${state.labCaptureStats.dnsQueryCount}"
        lines += "DNS upstream: ${state.labCaptureStats.dnsUpstreamHost}:${state.labCaptureStats.dnsUpstreamPort}"
        lines += "DNS blocked responses: ${state.labCaptureStats.dnsBlockedResponseCount}"
        lines += "DNS-only blocked responses: ${state.labCaptureStats.dnsOnlyBlockedResponseCount}"
        lines += "DNS allowed-but-dropped: ${state.labCaptureStats.dnsAllowedDroppedCount}"
        lines += "DNS allowed forwarded: ${state.labCaptureStats.dnsAllowedForwardedCount}"
        lines += "DNS-only allowed forwarded: ${state.labCaptureStats.dnsOnlyAllowedForwardedCount}"
        lines += "DNS allowed forward failures: ${state.labCaptureStats.dnsAllowedForwardFailureCount}"
        lines += "DNS-only allowed forward failures: ${state.labCaptureStats.dnsOnlyAllowedForwardFailureCount}"
        lines += "DNS allowed forward timeouts: ${state.labCaptureStats.dnsAllowedForwardTimeoutCount}"
        lines += "DNS alert-only dropped: ${state.labCaptureStats.dnsAlertDroppedCount}"
        lines += "DNS response write successes: ${state.labCaptureStats.dnsResponseWriteSuccessCount}"
        lines += "DNS response write failures: ${state.labCaptureStats.dnsResponseWriteFailureCount}"
        lines += "Allowed/blocked/alert: ${state.labCaptureStats.allowedDomainCount}/" +
            "${state.labCaptureStats.blockedDomainCount}/${state.labCaptureStats.alertOnlyDomainCount}"
        lines += "Malformed packet/DNS: ${state.labCaptureStats.malformedPacketCount}/${state.labCaptureStats.malformedDnsCount}"
        lines += "Last packet summary: ${state.labCaptureStats.lastPacketSummary ?: "none"}"
        if (state.labCaptureStats.recentDnsObservations.isNotEmpty()) {
            lines += "Local lab DNS observations from this test session:"
            state.labCaptureStats.recentDnsObservations.take(5).forEach { observation ->
                lines += observation.summary()
            }
        }
        lines += ChildVpnSmokeLabels.LAB_LOCAL_ONLY
        lines += ChildVpnSmokeLabels.LAB_DNS_LOCAL_ONLY
        lines += ChildVpnSmokeLabels.LAB_DNS_SINKHOLE
        lines += ChildVpnSmokeLabels.LAB_ALLOWED_DROPPED
        lines += ChildVpnSmokeLabels.LAB_ALLOWED_NON_DNS_DROPPED
        lines += ChildVpnSmokeLabels.DNS_ONLY_NON_DNS
        lines += ChildVpnSmokeLabels.DNS_ONLY_DOH_WARNING
        lines += ChildVpnSmokeLabels.LAB_INTERNET_MAY_NOT_WORK
        lines += ChildVpnSmokeLabels.LAB_NOT_FULL_PROTECTION
        lines += ChildVpnSmokeLabels.LAB_AUTO_STOP
        lines += "Local debug events: ${state.localEvents.size}"
        lines += "Filtering enabled: no"
        return lines.joinToString(separator = "\n")
    }
}
