package com.vordain.guard.vpn.engine

import com.vordain.guard.core.intelligence.AppCompatibilityProfile
import com.vordain.guard.core.intelligence.DomainIntelligenceCategory
import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.AppTrafficMode
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.policy.Policy
import com.vordain.guard.core.policy.PolicyDecisionReason

class DefaultAppAwareTrafficGate(
    private val domainTrafficEvaluator: DomainTrafficEvaluator,
    private val compatibilityDecisionCache: CompatibilityDecisionCache,
    private val compatibilityCacheTtlMillis: Long,
    private val appCompatibilityProfileProvider: AppCompatibilityProfileProvider = EmptyAppCompatibilityProfileProvider,
    private val domainIntelligenceProvider: DomainIntelligenceProvider = EmptyDomainIntelligenceProvider,
) : AppAwareTrafficGate {
    init {
        require(compatibilityCacheTtlMillis >= 0L) { "compatibilityCacheTtlMillis must be non-negative" }
    }

    override fun evaluate(
        observation: TrafficObservation,
        policy: Policy,
        appMode: AppTrafficMode,
    ): AppAwareTrafficGateDecision {
        if (appMode == AppTrafficMode.BLOCKED) {
            return decision(
                action = TrafficAction.BLOCK,
                reason = AppAwareTrafficGateReason.APP_BLOCKED,
                shouldCreateEvent = true,
            )
        }

        val domainName = observation.domainName ?: return missingDomainDecision(appMode)
        val trafficDecision = domainTrafficEvaluator.evaluateDomain(domainName, policy)
        val intelligenceCategories = domainIntelligenceProvider.categoriesFor(domainName)
        val profile = observation.appPackageName?.let(appCompatibilityProfileProvider::profileFor)

        if (trafficDecision.isHardSafetyBlock() || intelligenceCategories.containsHardSafetyCategory()) {
            return decision(
                action = TrafficAction.BLOCK,
                reason = AppAwareTrafficGateReason.HARD_SAFETY_BLOCK,
                shouldCreateEvent = true,
                trafficDecision = trafficDecision,
            )
        }

        return when (appMode) {
            AppTrafficMode.STRICT -> fromDomainPolicy(trafficDecision)
            AppTrafficMode.COMPATIBILITY -> compatibilityDecision(
                observation = observation,
                domainName = domainName,
                trafficDecision = trafficDecision,
                profile = profile,
            )
            AppTrafficMode.MONITOR -> monitorDecision(trafficDecision)
            AppTrafficMode.BLOCKED -> error("BLOCKED mode is handled before domain evaluation")
        }
    }

    private fun missingDomainDecision(appMode: AppTrafficMode): AppAwareTrafficGateDecision {
        return when (appMode) {
            AppTrafficMode.STRICT -> decision(
                action = TrafficAction.BLOCK,
                reason = AppAwareTrafficGateReason.MISSING_DOMAIN,
                shouldCreateEvent = true,
            )
            AppTrafficMode.COMPATIBILITY -> decision(
                action = TrafficAction.ALLOW,
                reason = AppAwareTrafficGateReason.MISSING_DOMAIN,
                shouldCreateEvent = false,
            )
            AppTrafficMode.MONITOR -> decision(
                action = TrafficAction.ALERT_ONLY,
                reason = AppAwareTrafficGateReason.MISSING_DOMAIN,
                shouldCreateEvent = true,
            )
            AppTrafficMode.BLOCKED -> decision(
                action = TrafficAction.BLOCK,
                reason = AppAwareTrafficGateReason.APP_BLOCKED,
                shouldCreateEvent = true,
            )
        }
    }

    private fun fromDomainPolicy(trafficDecision: TrafficDecision): AppAwareTrafficGateDecision {
        return decision(
            action = trafficDecision.action,
            reason = when (trafficDecision.action) {
                TrafficAction.ALLOW -> AppAwareTrafficGateReason.DOMAIN_POLICY_ALLOWED
                TrafficAction.BLOCK -> AppAwareTrafficGateReason.DOMAIN_POLICY_BLOCKED
                TrafficAction.ALERT_ONLY -> AppAwareTrafficGateReason.DOMAIN_POLICY_ALERT_ONLY
            },
            shouldCreateEvent = trafficDecision.evaluation.shouldCreateEvent,
            trafficDecision = trafficDecision,
        )
    }

    private fun compatibilityDecision(
        observation: TrafficObservation,
        domainName: DomainName,
        trafficDecision: TrafficDecision,
        profile: AppCompatibilityProfile?,
    ): AppAwareTrafficGateDecision {
        val appPackageName = observation.appPackageName
        if (appPackageName != null) {
            val cacheEntry = compatibilityDecisionCache.get(
                appPackageName = appPackageName,
                domainName = domainName,
                policyVersion = observation.policyVersion,
                currentTimeMillis = observation.observedAtMillis,
            )
            if (cacheEntry != null) {
                return decision(
                    action = cacheEntry.action,
                    reason = AppAwareTrafficGateReason.COMPATIBILITY_CACHE_HIT,
                    cacheTtlMillis = cacheEntry.expiresAtMillis - observation.observedAtMillis,
                    shouldCreateEvent = false,
                    trafficDecision = trafficDecision,
                )
            }
        }

        if (profile != null && profile.blockedRegardlessDomains.any { rule -> matches(candidate = domainName, rule = rule) }) {
            return decision(
                action = TrafficAction.BLOCK,
                reason = AppAwareTrafficGateReason.HARD_SAFETY_BLOCK,
                shouldCreateEvent = true,
                trafficDecision = trafficDecision,
            )
        }

        if (profile != null && profile.compatibilityDomains().any { rule -> matches(candidate = domainName, rule = rule) }) {
            cacheAllowIfPossible(
                appPackageName = appPackageName,
                domainName = domainName,
                policyVersion = observation.policyVersion,
                observedAtMillis = observation.observedAtMillis,
                action = TrafficAction.ALLOW,
                reason = AppAwareTrafficGateReason.COMPATIBILITY_LEARNED_ALLOW,
            )
            return decision(
                action = TrafficAction.ALLOW,
                reason = AppAwareTrafficGateReason.COMPATIBILITY_LEARNED_ALLOW,
                cacheTtlMillis = compatibilityCacheTtlMillis,
                shouldCreateEvent = false,
                trafficDecision = trafficDecision,
            )
        }

        return when {
            trafficDecision.action == TrafficAction.ALLOW -> {
                cacheAllowIfPossible(
                    appPackageName = appPackageName,
                    domainName = domainName,
                    policyVersion = observation.policyVersion,
                    observedAtMillis = observation.observedAtMillis,
                    action = TrafficAction.ALLOW,
                    reason = AppAwareTrafficGateReason.DOMAIN_POLICY_ALLOWED,
                )
                decision(
                    action = TrafficAction.ALLOW,
                    reason = AppAwareTrafficGateReason.DOMAIN_POLICY_ALLOWED,
                    cacheTtlMillis = compatibilityCacheTtlMillis,
                    shouldCreateEvent = false,
                    trafficDecision = trafficDecision,
                )
            }
            trafficDecision.action == TrafficAction.ALERT_ONLY -> {
                cacheAllowIfPossible(
                    appPackageName = appPackageName,
                    domainName = domainName,
                    policyVersion = observation.policyVersion,
                    observedAtMillis = observation.observedAtMillis,
                    action = TrafficAction.ALERT_ONLY,
                    reason = AppAwareTrafficGateReason.DOMAIN_POLICY_ALERT_ONLY,
                )
                decision(
                    action = TrafficAction.ALERT_ONLY,
                    reason = AppAwareTrafficGateReason.DOMAIN_POLICY_ALERT_ONLY,
                    cacheTtlMillis = compatibilityCacheTtlMillis,
                    shouldCreateEvent = trafficDecision.evaluation.shouldCreateEvent,
                    trafficDecision = trafficDecision,
                )
            }
            trafficDecision.evaluation.reason == PolicyDecisionReason.UNKNOWN_DOMAIN_BLOCKED -> {
                cacheAllowIfPossible(
                    appPackageName = appPackageName,
                    domainName = domainName,
                    policyVersion = observation.policyVersion,
                    observedAtMillis = observation.observedAtMillis,
                    action = TrafficAction.ALLOW,
                    reason = AppAwareTrafficGateReason.COMPATIBILITY_LEARNED_ALLOW,
                )
                decision(
                    action = TrafficAction.ALLOW,
                    reason = AppAwareTrafficGateReason.COMPATIBILITY_LEARNED_ALLOW,
                    cacheTtlMillis = compatibilityCacheTtlMillis,
                    shouldCreateEvent = false,
                    trafficDecision = trafficDecision,
                )
            }
            else -> fromDomainPolicy(trafficDecision)
        }
    }

    private fun monitorDecision(trafficDecision: TrafficDecision): AppAwareTrafficGateDecision {
        return decision(
            action = if (trafficDecision.action == TrafficAction.BLOCK) TrafficAction.ALERT_ONLY else trafficDecision.action,
            reason = AppAwareTrafficGateReason.MONITOR_MODE,
            shouldCreateEvent = true,
            trafficDecision = trafficDecision,
        )
    }

    private fun cacheAllowIfPossible(
        appPackageName: AppPackageName?,
        domainName: DomainName,
        policyVersion: String?,
        observedAtMillis: Long,
        action: TrafficAction,
        reason: AppAwareTrafficGateReason,
    ) {
        if (appPackageName == null || compatibilityCacheTtlMillis == 0L) {
            return
        }
        compatibilityDecisionCache.put(
            CompatibilityCacheEntry(
                appPackageName = appPackageName,
                domainName = domainName,
                policyVersion = policyVersion,
                action = action,
                reason = reason,
                createdAtMillis = observedAtMillis,
                expiresAtMillis = observedAtMillis + compatibilityCacheTtlMillis,
            ),
            )
    }

    private fun AppCompatibilityProfile.compatibilityDomains(): Set<DomainName> {
        return requiredDomains + optionalDomains
    }

    private fun Set<DomainIntelligenceCategory>.containsHardSafetyCategory(): Boolean {
        return any { category -> category in hardSafetyIntelligenceCategories }
    }

    private fun matches(candidate: DomainName, rule: DomainName): Boolean {
        return candidate == rule || candidate.value.endsWith(".${rule.value}")
    }

    private fun TrafficDecision.isHardSafetyBlock(): Boolean {
        return action == TrafficAction.BLOCK &&
            evaluation.reason in setOf(
                PolicyDecisionReason.BLOCKLIST_MATCH,
                PolicyDecisionReason.PROXY_CATEGORY_BLOCKED,
                PolicyDecisionReason.LOCKDOWN_MODE,
            )
    }

    private fun decision(
        action: TrafficAction,
        reason: AppAwareTrafficGateReason,
        cacheTtlMillis: Long? = null,
        shouldCreateEvent: Boolean,
        trafficDecision: TrafficDecision? = null,
    ): AppAwareTrafficGateDecision {
        return AppAwareTrafficGateDecision(
            action = action,
            reason = reason,
            cacheTtlMillis = cacheTtlMillis,
            shouldCreateEvent = shouldCreateEvent,
            trafficDecision = trafficDecision,
        )
    }

    private object EmptyAppCompatibilityProfileProvider : AppCompatibilityProfileProvider {
        override fun profileFor(appPackageName: AppPackageName): AppCompatibilityProfile? = null
    }

    private object EmptyDomainIntelligenceProvider : DomainIntelligenceProvider {
        override fun recordFor(domain: DomainName) = null
        override fun categoriesFor(domain: DomainName): Set<DomainIntelligenceCategory> = emptySet()
    }

    private companion object {
        val hardSafetyIntelligenceCategories = setOf(
            DomainIntelligenceCategory.PROXY_ANONYMIZER,
            DomainIntelligenceCategory.PRIVATE_DNS,
            DomainIntelligenceCategory.VPN_INFRASTRUCTURE,
        )
    }
}
