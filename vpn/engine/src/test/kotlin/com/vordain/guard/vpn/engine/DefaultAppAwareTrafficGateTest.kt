package com.vordain.guard.vpn.engine

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.AppTrafficMode
import com.vordain.guard.core.model.DomainCategory
import com.vordain.guard.core.model.DomainClassification
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.policy.DefaultPolicyEngine
import com.vordain.guard.core.policy.Policy
import com.vordain.guard.core.policy.PolicyDecisionReason
import com.vordain.guard.vpn.classifier.DomainClassifier
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultAppAwareTrafficGateTest {
    @Test
    fun blockedAppModeBlocks() {
        val decision = fixture().gate.evaluate(
            observation = observation(domain = "unknown.example"),
            policy = standardPolicy(),
            appMode = AppTrafficMode.BLOCKED,
        )

        assertEquals(TrafficAction.BLOCK, decision.action)
        assertEquals(AppAwareTrafficGateReason.APP_BLOCKED, decision.reason)
        assertTrue(decision.shouldCreateEvent)
    }

    @Test
    fun strictModeBlocksUnknownDomainWhenPolicyBlocksUnknownDomains() {
        val decision = fixture().gate.evaluate(
            observation = observation(domain = "unknown.example"),
            policy = standardPolicy(blockUnknownDomains = true),
            appMode = AppTrafficMode.STRICT,
        )

        assertEquals(TrafficAction.BLOCK, decision.action)
        assertEquals(AppAwareTrafficGateReason.DOMAIN_POLICY_BLOCKED, decision.reason)
        assertEquals(PolicyDecisionReason.UNKNOWN_DOMAIN_BLOCKED, decision.trafficDecision?.evaluation?.reason)
    }

    @Test
    fun compatibilityModeAllowsUnknownDomainBlockReasonAndCachesIt() {
        val fixture = fixture()
        val decision = fixture.gate.evaluate(
            observation = observation(domain = "service.streaming.example"),
            policy = standardPolicy(blockUnknownDomains = true),
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.ALLOW, decision.action)
        assertEquals(AppAwareTrafficGateReason.COMPATIBILITY_LEARNED_ALLOW, decision.reason)
        assertEquals(false, decision.shouldCreateEvent)
        assertNotNull(
            fixture.cache.get(
                appPackageName = streamingApp,
                domainName = DomainName.from("service.streaming.example"),
                policyVersion = "policy-1",
                currentTimeMillis = 1_100L,
            ),
        )
    }

    @Test
    fun compatibilityModeLaterHitsCacheForSameAppDomainAndPolicyVersion() {
        val fixture = fixture()
        val policy = standardPolicy(blockUnknownDomains = true)

        fixture.gate.evaluate(observation = observation(domain = "service.streaming.example"), policy = policy, appMode = AppTrafficMode.COMPATIBILITY)
        val secondDecision = fixture.gate.evaluate(
            observation = observation(domain = "service.streaming.example", observedAtMillis = 1_200L),
            policy = policy,
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.ALLOW, secondDecision.action)
        assertEquals(AppAwareTrafficGateReason.COMPATIBILITY_CACHE_HIT, secondDecision.reason)
    }

    @Test
    fun compatibilityCacheDoesNotApplyToAnotherApp() {
        val fixture = fixture()
        val policy = standardPolicy(blockUnknownDomains = true)

        fixture.gate.evaluate(observation = observation(domain = "service.streaming.example"), policy = policy, appMode = AppTrafficMode.COMPATIBILITY)
        val otherAppDecision = fixture.gate.evaluate(
            observation = observation(
                app = AppPackageName("com.other.streaming"),
                domain = "service.streaming.example",
                observedAtMillis = 1_200L,
            ),
            policy = policy,
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(AppAwareTrafficGateReason.COMPATIBILITY_LEARNED_ALLOW, otherAppDecision.reason)
    }

    @Test
    fun compatibilityCacheDoesNotApplyAfterPolicyVersionChanges() {
        val fixture = fixture()
        val policy = standardPolicy(blockUnknownDomains = true)

        fixture.gate.evaluate(observation = observation(domain = "service.streaming.example"), policy = policy, appMode = AppTrafficMode.COMPATIBILITY)
        val changedPolicyDecision = fixture.gate.evaluate(
            observation = observation(
                domain = "service.streaming.example",
                policyVersion = "policy-2",
                observedAtMillis = 1_200L,
            ),
            policy = policy,
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(AppAwareTrafficGateReason.COMPATIBILITY_LEARNED_ALLOW, changedPolicyDecision.reason)
    }

    @Test
    fun compatibilityModeDoesNotOverrideExplicitBlocklist() {
        val decision = fixture().gate.evaluate(
            observation = observation(domain = "blocked.example"),
            policy = standardPolicy(blockedDomains = setOf(DomainName.from("blocked.example"))),
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.BLOCK, decision.action)
        assertEquals(AppAwareTrafficGateReason.HARD_SAFETY_BLOCK, decision.reason)
        assertEquals(PolicyDecisionReason.BLOCKLIST_MATCH, decision.trafficDecision?.evaluation?.reason)
    }

    @Test
    fun compatibilityModeDoesNotOverrideProxyAnonymizerBlock() {
        val decision = fixture(proxyDomains = setOf(DomainName.from("proxy.example"))).gate.evaluate(
            observation = observation(domain = "proxy.example"),
            policy = standardPolicy(blockKnownProxyDomains = true),
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.BLOCK, decision.action)
        assertEquals(AppAwareTrafficGateReason.HARD_SAFETY_BLOCK, decision.reason)
        assertEquals(PolicyDecisionReason.PROXY_CATEGORY_BLOCKED, decision.trafficDecision?.evaluation?.reason)
    }

    @Test
    fun compatibilityModeDoesNotOverrideCrisisLockdown() {
        val decision = fixture().gate.evaluate(
            observation = observation(domain = "service.streaming.example"),
            policy = standardPolicy(mode = LockdownMode.CRISIS_LOCKDOWN, blockUnknownDomains = false),
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.BLOCK, decision.action)
        assertEquals(AppAwareTrafficGateReason.HARD_SAFETY_BLOCK, decision.reason)
        assertEquals(PolicyDecisionReason.LOCKDOWN_MODE, decision.trafficDecision?.evaluation?.reason)
    }

    @Test
    fun compatibilityModeAllowsNormalAllowlistedDomain() {
        val decision = fixture().gate.evaluate(
            observation = observation(domain = "media.example"),
            policy = standardPolicy(allowedDomains = setOf(DomainName.from("media.example"))),
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.ALLOW, decision.action)
        assertEquals(AppAwareTrafficGateReason.DOMAIN_POLICY_ALLOWED, decision.reason)
        assertEquals(false, decision.shouldCreateEvent)
    }

    @Test
    fun compatibilityCachePreservesAlertOnlyAction() {
        val fixture = fixture()
        val policy = standardPolicy(mode = LockdownMode.MONITOR_ONLY, blockUnknownDomains = false)

        val firstDecision = fixture.gate.evaluate(
            observation = observation(domain = "learning.example"),
            policy = policy,
            appMode = AppTrafficMode.COMPATIBILITY,
        )
        val cachedDecision = fixture.gate.evaluate(
            observation = observation(domain = "learning.example", observedAtMillis = 1_200L),
            policy = policy,
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.ALERT_ONLY, firstDecision.action)
        assertEquals(AppAwareTrafficGateReason.DOMAIN_POLICY_ALERT_ONLY, firstDecision.reason)
        assertEquals(TrafficAction.ALERT_ONLY, cachedDecision.action)
        assertEquals(AppAwareTrafficGateReason.COMPATIBILITY_CACHE_HIT, cachedDecision.reason)
    }

    @Test
    fun monitorModeCreatesParentVisibleWarningForNonHardBlockTraffic() {
        val decision = fixture().gate.evaluate(
            observation = observation(domain = "unknown.example"),
            policy = standardPolicy(blockUnknownDomains = true),
            appMode = AppTrafficMode.MONITOR,
        )

        assertEquals(TrafficAction.ALERT_ONLY, decision.action)
        assertEquals(AppAwareTrafficGateReason.MONITOR_MODE, decision.reason)
        assertTrue(decision.shouldCreateEvent)
    }

    @Test
    fun missingDomainBehaviorIsExplicitForAllModes() {
        val fixture = fixture()
        val policy = standardPolicy()

        val strict = fixture.gate.evaluate(observation = observation(domain = null), policy = policy, appMode = AppTrafficMode.STRICT)
        val compatibility = fixture.gate.evaluate(observation = observation(domain = null), policy = policy, appMode = AppTrafficMode.COMPATIBILITY)
        val blocked = fixture.gate.evaluate(observation = observation(domain = null), policy = policy, appMode = AppTrafficMode.BLOCKED)
        val monitor = fixture.gate.evaluate(observation = observation(domain = null), policy = policy, appMode = AppTrafficMode.MONITOR)

        assertEquals(TrafficAction.BLOCK, strict.action)
        assertEquals(AppAwareTrafficGateReason.MISSING_DOMAIN, strict.reason)
        assertEquals(TrafficAction.ALLOW, compatibility.action)
        assertEquals(AppAwareTrafficGateReason.MISSING_DOMAIN, compatibility.reason)
        assertEquals(TrafficAction.BLOCK, blocked.action)
        assertEquals(AppAwareTrafficGateReason.APP_BLOCKED, blocked.reason)
        assertEquals(TrafficAction.ALERT_ONLY, monitor.action)
        assertEquals(AppAwareTrafficGateReason.MISSING_DOMAIN, monitor.reason)
    }

    @Test
    fun engineSourceDoesNotImportForbiddenPackages() {
        val sourceRoot = repositoryRoot().resolve("vpn/engine/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn", "service").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "features").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
    }

    private fun fixture(proxyDomains: Set<DomainName> = emptySet()): Fixture {
        val cache = InMemoryCompatibilityDecisionCache()
        val classifier = TestDomainClassifier(proxyDomains)
        val domainTrafficEvaluator = DefaultDomainTrafficEvaluator(
            domainClassifier = classifier,
            policyEngine = DefaultPolicyEngine(),
        )
        val gate = DefaultAppAwareTrafficGate(
            domainTrafficEvaluator = domainTrafficEvaluator,
            compatibilityDecisionCache = cache,
            compatibilityCacheTtlMillis = 5_000L,
        )
        return Fixture(gate = gate, cache = cache)
    }

    private fun observation(
        app: AppPackageName? = streamingApp,
        domain: String?,
        observedAtMillis: Long = 1_000L,
        policyVersion: String? = "policy-1",
    ): TrafficObservation {
        return TrafficObservation(
            appPackageName = app,
            domainName = domain?.let(DomainName::from),
            observedAtMillis = observedAtMillis,
            policyVersion = policyVersion,
        )
    }

    private fun standardPolicy(
        mode: LockdownMode = LockdownMode.STANDARD,
        allowedDomains: Set<DomainName> = emptySet(),
        blockedDomains: Set<DomainName> = emptySet(),
        blockUnknownDomains: Boolean = true,
        blockKnownProxyDomains: Boolean = false,
    ): Policy {
        return Policy(
            id = PolicyId("policy-1"),
            mode = mode,
            allowedDomains = allowedDomains,
            blockedDomains = blockedDomains,
            allowedPackages = emptySet(),
            blockedPackages = emptySet(),
            blockUnknownDomains = blockUnknownDomains,
            blockKnownProxyDomains = blockKnownProxyDomains,
        )
    }

    private class TestDomainClassifier(private val proxyDomains: Set<DomainName>) : DomainClassifier {
        override fun classify(domain: DomainName): DomainClassification {
            return if (domain in proxyDomains) {
                DomainClassification.of(DomainCategory.PROXY_ANONYMIZER)
            } else {
                DomainClassification.Unknown
            }
        }
    }

    private data class Fixture(
        val gate: DefaultAppAwareTrafficGate,
        val cache: InMemoryCompatibilityDecisionCache,
    )

    private fun assertSourceTreeDoesNotContain(sourceRoot: File, forbiddenText: String) {
        val filesWithForbiddenText = kotlinFilesUnder(sourceRoot).filter { file ->
            file.readText().contains(forbiddenText)
        }

        assertTrue(
            actual = filesWithForbiddenText.isEmpty(),
            message = "Forbidden text $forbiddenText found in ${filesWithForbiddenText.map { it.path }}",
        )
    }

    private fun kotlinFilesUnder(sourceRoot: File): List<File> {
        val kotlinFiles = sourceRoot.walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .toList()

        assertTrue(kotlinFiles.isNotEmpty(), "Expected Kotlin files under ${sourceRoot.path}")
        return kotlinFiles
    }

    private fun repositoryRoot(): File {
        var current = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) {
                return current
            }
            current = current.parentFile ?: error("Could not find repository root from ${System.getProperty("user.dir")}")
        }
    }

    private companion object {
        val streamingApp = AppPackageName("com.streaming.app")
    }
}
