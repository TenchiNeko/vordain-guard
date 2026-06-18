package com.vordain.guard.core.policy

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName

interface PolicyEngine {
    fun evaluateDomain(domain: DomainName, policy: Policy): PolicyEvaluation

    fun evaluateApp(packageName: AppPackageName, policy: Policy): PolicyEvaluation
}
