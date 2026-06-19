package com.vordain.guard.child

import com.vordain.guard.vpn.service.TunPacketCaptureStats

data class ChildDebugDashboardState(
    val vpnPermissionStatus: String,
    val lastCommand: String,
    val shellStatus: String,
    val setupChecklist: VpnSetupChecklist,
    val policyResult: ChildDebugPolicyResult?,
    val policyHandoffResult: ChildDebugPolicyHandoffResult?,
    val currentPolicyVersion: String,
    val compatibilityResult: ChildDebugCompatibilityResult?,
    val reviewResult: ChildDebugReviewResult?,
    val localEvents: List<String>,
    val labCaptureStats: TunPacketCaptureStats,
)
