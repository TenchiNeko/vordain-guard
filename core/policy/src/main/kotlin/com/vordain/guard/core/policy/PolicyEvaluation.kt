package com.vordain.guard.core.policy

data class PolicyEvaluation(
    val decision: PolicyDecision,
    val reason: PolicyDecisionReason,
    val shouldCreateEvent: Boolean,
)
