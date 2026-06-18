package com.vordain.guard.vpn.engine

import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.policy.Policy
import com.vordain.guard.core.policy.PolicyDecision
import com.vordain.guard.core.policy.PolicyEngine
import com.vordain.guard.vpn.classifier.DomainClassifier

class DefaultDomainTrafficEvaluator(
    private val domainClassifier: DomainClassifier,
    private val policyEngine: PolicyEngine,
) : DomainTrafficEvaluator {
    override fun evaluateDomain(domain: DomainName, policy: Policy): TrafficDecision {
        val classification = domainClassifier.classify(domain)
        val evaluation = policyEngine.evaluateDomain(
            domain = domain,
            policy = policy,
            classification = classification,
        )

        return TrafficDecision(
            action = evaluation.toTrafficAction(),
            evaluation = evaluation,
        )
    }

    private fun com.vordain.guard.core.policy.PolicyEvaluation.toTrafficAction(): TrafficAction {
        return when (decision) {
            PolicyDecision.Allow -> TrafficAction.ALLOW
            PolicyDecision.Block -> TrafficAction.BLOCK
            PolicyDecision.AlertOnly -> TrafficAction.ALERT_ONLY
        }
    }
}
