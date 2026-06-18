package com.vordain.guard.vpn.engine

import com.vordain.guard.core.policy.Policy
import com.vordain.guard.vpn.dns.DnsMessageParser
import com.vordain.guard.vpn.dns.DnsParseResult

class DefaultDnsTrafficEvaluator(
    private val dnsMessageParser: DnsMessageParser,
    private val domainTrafficEvaluator: DomainTrafficEvaluator,
) : DnsTrafficEvaluator {
    override fun evaluateDnsMessage(message: ByteArray, policy: Policy): DnsTrafficDecision {
        return when (val parseResult = dnsMessageParser.parse(message)) {
            is DnsParseResult.Failure -> DnsTrafficDecision.ParseFailure(parseResult)
            is DnsParseResult.Success -> aggregate(
                parseResult.questions.map { question ->
                    DnsQuestionDecision(
                        question = question,
                        trafficDecision = domainTrafficEvaluator.evaluateDomain(question.domain, policy),
                    )
                },
            )
        }
    }

    private fun aggregate(questionDecisions: List<DnsQuestionDecision>): DnsTrafficDecision {
        return when {
            questionDecisions.any { it.trafficDecision.action == TrafficAction.BLOCK } ->
                DnsTrafficDecision.Block(questionDecisions)
            questionDecisions.any { it.trafficDecision.action == TrafficAction.ALERT_ONLY } ->
                DnsTrafficDecision.AlertOnly(questionDecisions)
            else -> DnsTrafficDecision.Allow(questionDecisions)
        }
    }
}
