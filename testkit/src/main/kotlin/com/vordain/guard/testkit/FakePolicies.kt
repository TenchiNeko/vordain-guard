package com.vordain.guard.testkit

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.policy.Policy

object FakePolicies {
    fun standard(): Policy {
        return Policy(
            id = PolicyId("test-policy"),
            mode = LockdownMode.STANDARD,
            allowedDomains = setOf(DomainName.from("school.example")),
            blockedDomains = setOf(DomainName.from("proxy.example")),
            allowedPackages = setOf(AppPackageName("com.school.app")),
            blockedPackages = setOf(AppPackageName("com.proxy.app")),
            blockUnknownDomains = false,
            blockKnownProxyDomains = true,
        )
    }
}
