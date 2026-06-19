package com.vordain.guard.child

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.AppTrafficMode
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.policy.DefaultPolicyEngine
import com.vordain.guard.core.policy.Policy
import com.vordain.guard.core.policy.PolicyDecisionReason
import com.vordain.guard.vpn.classifier.StaticRuleListClassifier
import com.vordain.guard.vpn.engine.DefaultAppAwareTrafficGate
import com.vordain.guard.vpn.engine.DefaultDomainTrafficEvaluator
import com.vordain.guard.vpn.engine.InMemoryCompatibilityDecisionCache
import com.vordain.guard.vpn.engine.TrafficAction
import com.vordain.guard.vpn.engine.TrafficObservation

class ChildDebugCompatibilityDemo(
    private val observedAtMillisProvider: () -> Long,
) {
    private val cache = InMemoryCompatibilityDecisionCache()
    private val gate = DefaultAppAwareTrafficGate(
        domainTrafficEvaluator = DefaultDomainTrafficEvaluator(
            domainClassifier = StaticRuleListClassifier(
                proxyAnonymizerRules = setOf(DomainName.from("proxy.example")),
            ),
            policyEngine = DefaultPolicyEngine(),
        ),
        compatibilityDecisionCache = cache,
        compatibilityCacheTtlMillis = 60_000L,
    )

    fun evaluate(
        rawPackageName: String,
        rawDomain: String,
        mode: AppTrafficMode,
    ): ChildDebugCompatibilityResult {
        return runCatching {
            val packageName = rawPackageName.trim().takeIf(String::isNotBlank)?.let(::AppPackageName)
            val domainName = DomainName.from(rawDomain)
            val decision = gate.evaluate(
                observation = TrafficObservation(
                    appPackageName = packageName,
                    domainName = domainName,
                    observedAtMillis = observedAtMillisProvider(),
                    policyVersion = DEBUG_POLICY_VERSION,
                ),
                policy = samplePolicy,
                appMode = mode,
            )
            val policyReason = decision.trafficDecision?.evaluation?.reason
            ChildDebugCompatibilityResult(
                packageName = packageName?.value ?: "not set",
                normalizedDomain = domainName.value,
                mode = mode.name,
                action = decision.action.name,
                reason = decision.reason.name,
                hardSafetyBlock = decision.action == TrafficAction.BLOCK && policyReason in hardSafetyReasons,
                cacheNote = if (decision.cacheTtlMillis != null) {
                    "cache ttl millis: ${decision.cacheTtlMillis}"
                } else {
                    "no cache entry"
                },
                error = null,
            )
        }.getOrElse { throwable ->
            ChildDebugCompatibilityResult(
                packageName = rawPackageName.ifBlank { "not set" },
                normalizedDomain = null,
                mode = mode.name,
                action = "Invalid",
                reason = "Input rejected",
                hardSafetyBlock = false,
                cacheNote = "not cached",
                error = throwable.message ?: "Compatibility demo could not evaluate input",
            )
        }
    }

    private companion object {
        const val DEBUG_POLICY_VERSION = "debug-tablet-policy-v1"

        val hardSafetyReasons = setOf(
            PolicyDecisionReason.BLOCKLIST_MATCH,
            PolicyDecisionReason.PROXY_CATEGORY_BLOCKED,
            PolicyDecisionReason.LOCKDOWN_MODE,
        )

        val samplePolicy = Policy(
            id = PolicyId("debug-compat-policy"),
            mode = LockdownMode.STANDARD,
            allowedDomains = setOf(DomainName.from("video.example")),
            blockedDomains = setOf(DomainName.from("blocked.example")),
            allowedPackages = emptySet(),
            blockedPackages = emptySet(),
            blockUnknownDomains = true,
            blockKnownProxyDomains = true,
        )
    }
}

data class ChildDebugCompatibilityResult(
    val packageName: String,
    val normalizedDomain: String?,
    val mode: String,
    val action: String,
    val reason: String,
    val hardSafetyBlock: Boolean,
    val cacheNote: String,
    val error: String?,
) {
    fun asDisplayText(): String {
        if (error != null) {
            return "Mode: $mode\nAction: $action\nReason: $reason\nError: $error"
        }

        return listOf(
            "Package: $packageName",
            "Normalized domain: $normalizedDomain",
            "Mode: $mode",
            "Action: $action",
            "Reason: $reason",
            "Hard safety block: ${if (hardSafetyBlock) "yes" else "no"}",
            "Cache note: $cacheNote",
        ).joinToString(separator = "\n")
    }
}
