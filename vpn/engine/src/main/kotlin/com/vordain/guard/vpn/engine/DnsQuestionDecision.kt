package com.vordain.guard.vpn.engine

import com.vordain.guard.vpn.dns.DnsQuestion

data class DnsQuestionDecision(
    val question: DnsQuestion,
    val trafficDecision: TrafficDecision,
)
