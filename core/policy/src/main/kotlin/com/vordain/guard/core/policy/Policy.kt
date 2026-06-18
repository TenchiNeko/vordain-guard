package com.vordain.guard.core.policy

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId

data class Policy(
    val id: PolicyId,
    val mode: LockdownMode,
    val allowedDomains: Set<DomainName>,
    val blockedDomains: Set<DomainName>,
    val allowedPackages: Set<AppPackageName>,
    val blockedPackages: Set<AppPackageName>,
    val blockUnknownDomains: Boolean,
    val blockKnownProxyDomains: Boolean,
)
