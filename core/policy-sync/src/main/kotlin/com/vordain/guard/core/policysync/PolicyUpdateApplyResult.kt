package com.vordain.guard.core.policysync

import com.vordain.guard.core.policy.Policy

data class PolicyUpdateApplyResult(
    val accepted: Boolean,
    val policy: Policy?,
    val policyVersion: PolicyVersion?,
    val reason: PolicyUpdateVerificationResult,
)
