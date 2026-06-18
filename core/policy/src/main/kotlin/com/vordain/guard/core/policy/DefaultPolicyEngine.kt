package com.vordain.guard.core.policy

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainCategory
import com.vordain.guard.core.model.DomainClassification
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode

class DefaultPolicyEngine : PolicyEngine {
    override fun evaluateDomain(domain: DomainName, policy: Policy): PolicyEvaluation {
        return evaluateDomain(
            domain = domain,
            policy = policy,
            classification = DomainClassification.Unknown,
        )
    }

    override fun evaluateDomain(
        domain: DomainName,
        policy: Policy,
        classification: DomainClassification,
    ): PolicyEvaluation {
        if (domain.value.isBlank()) {
            return PolicyEvaluation(
                decision = PolicyDecision.Block,
                reason = PolicyDecisionReason.INVALID_INPUT,
                shouldCreateEvent = true,
            )
        }

        if (policy.blockedDomains.any { rule -> DomainMatcher.matches(candidate = domain, rule = rule) }) {
            return PolicyEvaluation(
                decision = PolicyDecision.Block,
                reason = PolicyDecisionReason.BLOCKLIST_MATCH,
                shouldCreateEvent = true,
            )
        }

        if (policy.blockKnownProxyDomains && classification.contains(DomainCategory.PROXY_ANONYMIZER)) {
            return PolicyEvaluation(
                decision = PolicyDecision.Block,
                reason = PolicyDecisionReason.PROXY_CATEGORY_BLOCKED,
                shouldCreateEvent = true,
            )
        }

        if (policy.allowedDomains.any { rule -> DomainMatcher.matches(candidate = domain, rule = rule) }) {
            return PolicyEvaluation(
                decision = PolicyDecision.Allow,
                reason = PolicyDecisionReason.ALLOWLIST_MATCH,
                shouldCreateEvent = false,
            )
        }

        if (policy.mode == LockdownMode.CRISIS_LOCKDOWN) {
            return PolicyEvaluation(
                decision = PolicyDecision.Block,
                reason = PolicyDecisionReason.LOCKDOWN_MODE,
                shouldCreateEvent = true,
            )
        }

        if (policy.blockUnknownDomains) {
            return PolicyEvaluation(
                decision = PolicyDecision.Block,
                reason = PolicyDecisionReason.UNKNOWN_DOMAIN_BLOCKED,
                shouldCreateEvent = true,
            )
        }

        return PolicyEvaluation(
            decision = if (policy.mode == LockdownMode.MONITOR_ONLY) PolicyDecision.AlertOnly else PolicyDecision.Allow,
            reason = PolicyDecisionReason.NO_MATCH,
            shouldCreateEvent = policy.mode == LockdownMode.MONITOR_ONLY,
        )
    }

    override fun evaluateApp(packageName: AppPackageName, policy: Policy): PolicyEvaluation {
        if (packageName.value.isBlank()) {
            return PolicyEvaluation(
                decision = PolicyDecision.Block,
                reason = PolicyDecisionReason.INVALID_INPUT,
                shouldCreateEvent = true,
            )
        }

        if (packageName in policy.blockedPackages) {
            return PolicyEvaluation(
                decision = PolicyDecision.Block,
                reason = PolicyDecisionReason.BLOCKLIST_MATCH,
                shouldCreateEvent = true,
            )
        }

        if (packageName in policy.allowedPackages) {
            return PolicyEvaluation(
                decision = PolicyDecision.Allow,
                reason = PolicyDecisionReason.ALLOWLIST_MATCH,
                shouldCreateEvent = false,
            )
        }

        if (policy.mode == LockdownMode.CRISIS_LOCKDOWN) {
            return PolicyEvaluation(
                decision = PolicyDecision.Block,
                reason = PolicyDecisionReason.LOCKDOWN_MODE,
                shouldCreateEvent = true,
            )
        }

        return PolicyEvaluation(
            decision = PolicyDecision.Allow,
            reason = PolicyDecisionReason.NO_MATCH,
            shouldCreateEvent = false,
        )
    }
}
