package com.vordain.guard.core.policy

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainClassification
import com.vordain.guard.core.model.DomainName

interface PolicyEngine {
    fun evaluateDomain(domain: DomainName, policy: Policy): PolicyEvaluation

    fun evaluateDomain(
        domain: DomainName,
        policy: Policy,
        classification: DomainClassification,
    ): PolicyEvaluation

    fun evaluateApp(packageName: AppPackageName, policy: Policy): PolicyEvaluation
}
