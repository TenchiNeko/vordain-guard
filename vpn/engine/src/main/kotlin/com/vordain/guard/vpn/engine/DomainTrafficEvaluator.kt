package com.vordain.guard.vpn.engine

import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.policy.Policy

interface DomainTrafficEvaluator {
    fun evaluateDomain(domain: DomainName, policy: Policy): TrafficDecision
}
