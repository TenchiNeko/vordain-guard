package com.vordain.guard.core.policy

import com.vordain.guard.core.model.DomainName

data class PolicyPatch(
    val operation: PolicyPatchOperation,
    val domain: DomainName,
)
