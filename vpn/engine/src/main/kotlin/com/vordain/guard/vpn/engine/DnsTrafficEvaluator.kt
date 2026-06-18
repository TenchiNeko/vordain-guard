package com.vordain.guard.vpn.engine

import com.vordain.guard.core.policy.Policy

interface DnsTrafficEvaluator {
    fun evaluateDnsMessage(message: ByteArray, policy: Policy): DnsTrafficDecision
}
