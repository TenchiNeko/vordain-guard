package com.vordain.guard.core.policy

class PolicyPatchApplier {
    fun apply(policy: Policy, patch: PolicyPatch): Policy {
        return when (patch.operation) {
            PolicyPatchOperation.ADD_ALLOWED_DOMAIN -> policy.copy(
                allowedDomains = policy.allowedDomains + patch.domain,
            )
            PolicyPatchOperation.ADD_BLOCKED_DOMAIN -> policy.copy(
                blockedDomains = policy.blockedDomains + patch.domain,
            )
            PolicyPatchOperation.REMOVE_ALLOWED_DOMAIN -> policy.copy(
                allowedDomains = policy.allowedDomains - patch.domain,
            )
            PolicyPatchOperation.REMOVE_BLOCKED_DOMAIN -> policy.copy(
                blockedDomains = policy.blockedDomains - patch.domain,
            )
        }
    }
}
