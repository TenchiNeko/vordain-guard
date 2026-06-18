package com.vordain.guard.vpn.engine

import com.vordain.guard.core.intelligence.AppCompatibilityProfile
import com.vordain.guard.core.intelligence.DomainIntelligenceCategory
import com.vordain.guard.core.intelligence.DomainIntelligenceRecord
import com.vordain.guard.core.intelligence.DomainRiskLevel
import com.vordain.guard.core.intelligence.IntelligenceSource
import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.AppTrafficMode
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
import kotlin.test.assertTrue

class AppAwareTrafficGateIntelligenceTest {
    @Test
    fun compatibilityModeAllowsProfileRequiredDomain() {
        val decision = fixture(profile = streamingProfile()).gate.evaluate(
            observation = observation(domain = "media.streaming.example"),
            policy = standardPolicy(blockUnknownDomains = true),
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.ALLOW, decision.action)
        assertEquals(AppAwareTrafficGateReason.COMPATIBILITY_LEARNED_ALLOW, decision.reason)
    }

    @Test
    fun compatibilityModeAllowsProfileRequiredSubdomain() {
        val decision = fixture(profile = streamingProfile()).gate.evaluate(
            observation = observation(domain = "img.media.streaming.example"),
            policy = standardPolicy(blockUnknownDomains = true),
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.ALLOW, decision.action)
        assertEquals(AppAwareTrafficGateReason.COMPATIBILITY_LEARNED_ALLOW, decision.reason)
    }

    @Test
    fun profileBlockedRegardlessDomainBlocks() {
        val decision = fixture(profile = streamingProfile()).gate.evaluate(
            observation = observation(domain = "blocked.streaming.example"),
            policy = standardPolicy(blockUnknownDomains = false),
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.BLOCK, decision.action)
        assertEquals(AppAwareTrafficGateReason.HARD_SAFETY_BLOCK, decision.reason)
        assertTrue(decision.shouldCreateEvent)
    }

    @Test
    fun proxyAnonymizerIntelligenceBlocksInCompatibilityMode() {
        val decision = fixture(
            intelligenceRecords = listOf(record("proxy.example", DomainIntelligenceCategory.PROXY_ANONYMIZER)),
        ).gate.evaluate(
            observation = observation(domain = "proxy.example"),
            policy = standardPolicy(blockUnknownDomains = false),
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.BLOCK, decision.action)
        assertEquals(AppAwareTrafficGateReason.HARD_SAFETY_BLOCK, decision.reason)
    }

    @Test
    fun privateDnsIntelligenceBlocksInCompatibilityMode() {
        val decision = fixture(
            intelligenceRecords = listOf(record("dns.example", DomainIntelligenceCategory.PRIVATE_DNS)),
        ).gate.evaluate(
            observation = observation(domain = "dns.example"),
            policy = standardPolicy(blockUnknownDomains = false),
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.BLOCK, decision.action)
        assertEquals(AppAwareTrafficGateReason.HARD_SAFETY_BLOCK, decision.reason)
    }

    @Test
    fun existingBlocklistStillBlocksWithIntelligenceProviders() {
        val decision = fixture(profile = streamingProfile()).gate.evaluate(
            observation = observation(domain = "blocked.example"),
            policy = standardPolicy(blockedDomains = setOf(DomainName.from("blocked.example"))),
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.BLOCK, decision.action)
        assertEquals(AppAwareTrafficGateReason.HARD_SAFETY_BLOCK, decision.reason)
        assertEquals(PolicyDecisionReason.BLOCKLIST_MATCH, decision.trafficDecision?.evaluation?.reason)
    }

    @Test
    fun existingCrisisLockdownStillBlocksWithIntelligenceProviders() {
        val decision = fixture(profile = streamingProfile()).gate.evaluate(
            observation = observation(domain = "media.streaming.example"),
            policy = standardPolicy(mode = LockdownMode.CRISIS_LOCKDOWN, blockUnknownDomains = false),
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.BLOCK, decision.action)
        assertEquals(AppAwareTrafficGateReason.HARD_SAFETY_BLOCK, decision.reason)
        assertEquals(PolicyDecisionReason.LOCKDOWN_MODE, decision.trafficDecision?.evaluation?.reason)
    }

    @Test
    fun unknownDomainCompatibilityBehaviorRemainsUnchangedWithoutProfileOrIntelligence() {
        val decision = fixture().gate.evaluate(
            observation = observation(domain = "unknown.streaming.example"),
            policy = standardPolicy(blockUnknownDomains = true),
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(TrafficAction.ALLOW, decision.action)
        assertEquals(AppAwareTrafficGateReason.COMPATIBILITY_LEARNED_ALLOW, decision.reason)
    }

    @Test
    fun cacheStillRespectsAppDomainAndPolicyVersion() {
        val fixture = fixture(profile = streamingProfile())
        val policy = standardPolicy(blockUnknownDomains = true)

        fixture.gate.evaluate(
            observation = observation(domain = "media.streaming.example"),
            policy = policy,
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        val sameKey = fixture.gate.evaluate(
            observation = observation(domain = "media.streaming.example", observedAtMillis = 1_100L),
            policy = policy,
            appMode = AppTrafficMode.COMPATIBILITY,
        )
        val otherApp = fixture.gate.evaluate(
            observation = observation(app = AppPackageName("com.other.app"), domain = "media.streaming.example", observedAtMillis = 1_200L),
            policy = policy,
            appMode = AppTrafficMode.COMPATIBILITY,
        )
        val changedPolicy = fixture.gate.evaluate(
            observation = observation(domain = "media.streaming.example", observedAtMillis = 1_300L, policyVersion = "policy-2"),
            policy = policy,
            appMode = AppTrafficMode.COMPATIBILITY,
        )

        assertEquals(AppAwareTrafficGateReason.COMPATIBILITY_CACHE_HIT, sameKey.reason)
        assertEquals(AppAwareTrafficGateReason.COMPATIBILITY_LEARNED_ALLOW, otherApp.reason)
        assertEquals(AppAwareTrafficGateReason.COMPATIBILITY_LEARNED_ALLOW, changedPolicy.reason)
    }

    @Test
    fun engineAndIntelligenceSourcesKeepForbiddenDependenciesOut() {
        val engineSourceRoot = repositoryRoot().resolve("vpn/engine/src/main/kotlin")
        val intelligenceSourceRoot = repositoryRoot().resolve("core/intelligence/src/main/kotlin")

        assertSourceTreeDoesNotContain(engineSourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(engineSourceRoot, listOf("com", "vordain", "guard", "vpn", "service").joinToString("."))
        assertSourceTreeDoesNotContain(engineSourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(engineSourceRoot, listOf("com", "vordain", "guard", "data").joinToString("."))
        assertSourceTreeDoesNotContain(engineSourceRoot, listOf("com", "vordain", "guard", "features").joinToString("."))
        assertSourceTreeDoesNotContain(engineSourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
        assertSourceTreeDoesNotContain(intelligenceSourceRoot, listOf("com", "vordain", "guard", "vpn").joinToString("."))
        assertSourceTreeDoesNotContain(engineSourceRoot, "Manager")
        assertSourceTreeDoesNotContain(intelligenceSourceRoot, "Manager")
        assertSourceTreeDoesNotContain(engineSourceRoot, "TO" + "DO")
        assertSourceTreeDoesNotContain(intelligenceSourceRoot, "TO" + "DO")
        assertSourceTreeDoesNotContain(engineSourceRoot, "FIX" + "ME")
        assertSourceTreeDoesNotContain(intelligenceSourceRoot, "FIX" + "ME")
    }

    private fun fixture(
        profile: AppCompatibilityProfile? = null,
        intelligenceRecords: List<DomainIntelligenceRecord> = emptyList(),
    ): Fixture {
        val cache = InMemoryCompatibilityDecisionCache()
        val domainTrafficEvaluator = DefaultDomainTrafficEvaluator(
            domainClassifier = TestDomainClassifier(),
            policyEngine = DefaultPolicyEngine(),
        )
        val gate = DefaultAppAwareTrafficGate(
            domainTrafficEvaluator = domainTrafficEvaluator,
            compatibilityDecisionCache = cache,
            compatibilityCacheTtlMillis = 5_000L,
            appCompatibilityProfileProvider = InMemoryAppCompatibilityProfileProvider(listOfNotNull(profile)),
            domainIntelligenceProvider = InMemoryDomainIntelligenceProvider(intelligenceRecords),
        )
        return Fixture(gate = gate)
    }

    private fun streamingProfile(): AppCompatibilityProfile {
        return AppCompatibilityProfile(
            appPackageName = streamingApp,
            displayName = "Streaming App",
            requiredDomains = setOf(DomainName.from("media.streaming.example")),
            optionalDomains = setOf(DomainName.from("optional.streaming.example")),
            blockedRegardlessDomains = setOf(DomainName.from("blocked.streaming.example")),
            profileVersion = "profile-1",
            confidence = 90,
        )
    }

    private fun record(
        domain: String,
        category: DomainIntelligenceCategory,
    ): DomainIntelligenceRecord {
        return DomainIntelligenceRecord(
            domain = DomainName.from(domain),
            categories = setOf(category),
            riskLevel = DomainRiskLevel.UNKNOWN,
            confidence = 90,
            source = IntelligenceSource.HUMAN_REVIEW,
            reviewedAtMillis = 1_000L,
        )
    }

    private fun observation(
        app: AppPackageName? = streamingApp,
        domain: String,
        observedAtMillis: Long = 1_000L,
        policyVersion: String? = "policy-1",
    ): TrafficObservation {
        return TrafficObservation(
            appPackageName = app,
            domainName = DomainName.from(domain),
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

    private class TestDomainClassifier : DomainClassifier {
        override fun classify(domain: DomainName): DomainClassification {
            return DomainClassification.Unknown
        }
    }

    private data class Fixture(
        val gate: DefaultAppAwareTrafficGate,
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
