package com.vordain.guard.child

import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.policy.DefaultPolicyEngine
import com.vordain.guard.core.policy.Policy

class ChildDebugPolicyDemo(
    private val policyEngine: DefaultPolicyEngine = DefaultPolicyEngine(),
) {
    private var currentPolicy: Policy = defaultPolicy

    fun replacePolicy(policy: Policy) {
        currentPolicy = policy
    }

    fun policySummary(policyVersion: String): String {
        return listOf(
            "Current debug policy version: $policyVersion",
            "Allowed domains: ${currentPolicy.allowedDomains.size}",
            "Blocked domains: ${currentPolicy.blockedDomains.size}",
            "Mode: ${currentPolicy.mode.name}",
            "Block unknown domains: ${currentPolicy.blockUnknownDomains}",
            "Block known proxy domains: ${currentPolicy.blockKnownProxyDomains}",
        ).joinToString(separator = "\n")
    }

    fun evaluate(rawDomain: String): ChildDebugPolicyResult {
        return runCatching {
            val domainName = DomainName.from(rawDomain)
            val evaluation = policyEngine.evaluateDomain(domainName, currentPolicy)
            ChildDebugPolicyResult(
                normalizedDomain = domainName.value,
                decision = evaluation.decision.name,
                reason = evaluation.reason.name,
                shouldCreateEvent = evaluation.shouldCreateEvent,
                error = null,
            )
        }.getOrElse { throwable ->
            ChildDebugPolicyResult(
                normalizedDomain = null,
                decision = "Invalid",
                reason = "Input rejected",
                shouldCreateEvent = false,
                error = throwable.message ?: "Domain could not be evaluated",
            )
        }
    }

    private companion object {
        val defaultPolicy = Policy(
            id = PolicyId("debug-tablet-policy"),
            mode = LockdownMode.STANDARD,
            allowedDomains = setOf(DomainName.from("school.example.edu")),
            blockedDomains = setOf(DomainName.from("blocked.example")),
            allowedPackages = emptySet(),
            blockedPackages = emptySet(),
            blockUnknownDomains = false,
            blockKnownProxyDomains = true,
        )
    }
}

data class ChildDebugPolicyResult(
    val normalizedDomain: String?,
    val decision: String,
    val reason: String,
    val shouldCreateEvent: Boolean,
    val error: String?,
) {
    fun asDisplayText(): String {
        if (error != null) {
            return "Result: $decision\nReason: $reason\nError: $error"
        }

        return listOf(
            "Normalized domain: $normalizedDomain",
            "Decision: $decision",
            "Reason: $reason",
            "Event would be created: ${if (shouldCreateEvent) "yes" else "no"}",
        ).joinToString(separator = "\n")
    }
}
