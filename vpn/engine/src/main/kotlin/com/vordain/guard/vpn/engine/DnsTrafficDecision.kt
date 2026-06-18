package com.vordain.guard.vpn.engine

import com.vordain.guard.vpn.dns.DnsParseResult

sealed interface DnsTrafficDecision {
    data class Allow(val questionDecisions: List<DnsQuestionDecision>) : DnsTrafficDecision

    data class Block(val questionDecisions: List<DnsQuestionDecision>) : DnsTrafficDecision

    data class AlertOnly(val questionDecisions: List<DnsQuestionDecision>) : DnsTrafficDecision

    data class ParseFailure(val failure: DnsParseResult.Failure) : DnsTrafficDecision
}
