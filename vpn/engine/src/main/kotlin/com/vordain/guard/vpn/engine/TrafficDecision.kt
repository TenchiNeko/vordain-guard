package com.vordain.guard.vpn.engine

import com.vordain.guard.core.policy.PolicyEvaluation

data class TrafficDecision(
    val action: TrafficAction,
    val evaluation: PolicyEvaluation,
)

enum class TrafficAction {
    ALLOW,
    BLOCK,
    ALERT_ONLY,
}
