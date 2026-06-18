package com.vordain.guard.vpn.engine

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.policy.DefaultPolicyEngine
import com.vordain.guard.core.policy.Policy
import com.vordain.guard.core.policy.PolicyDecision
import com.vordain.guard.core.policy.PolicyDecisionReason
import com.vordain.guard.core.policy.PolicyEvaluation
import com.vordain.guard.vpn.classifier.StaticRuleListClassifier
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultDomainTrafficEvaluatorTest {
    @Test
    fun allowedDomainProducesAllowTrafficDecision() {
        val decision = evaluator().evaluateDomain(DomainName.from("school.example"), basePolicy())

        assertDecision(
            expectedAction = TrafficAction.ALLOW,
            expectedPolicyDecision = PolicyDecision.Allow,
            expectedReason = PolicyDecisionReason.ALLOWLIST_MATCH,
            expectedShouldCreateEvent = false,
            actual = decision,
        )
    }

    @Test
    fun blocklistedDomainProducesBlockTrafficDecision() {
        val decision = evaluator().evaluateDomain(DomainName.from("blocked.example"), basePolicy())

        assertDecision(
            expectedAction = TrafficAction.BLOCK,
            expectedPolicyDecision = PolicyDecision.Block,
            expectedReason = PolicyDecisionReason.BLOCKLIST_MATCH,
            expectedShouldCreateEvent = true,
            actual = decision,
        )
    }

    @Test
    fun proxyAnonymizerDomainBlocksWhenPolicyBlocksKnownProxyDomains() {
        val decision = evaluator(proxyRules = setOf(DomainName.from("proxy.example"))).evaluateDomain(
            domain = DomainName.from("login.proxy.example"),
            policy = basePolicy(blockKnownProxyDomains = true),
        )

        assertDecision(
            expectedAction = TrafficAction.BLOCK,
            expectedPolicyDecision = PolicyDecision.Block,
            expectedReason = PolicyDecisionReason.PROXY_CATEGORY_BLOCKED,
            expectedShouldCreateEvent = true,
            actual = decision,
        )
    }

    @Test
    fun proxyAnonymizerDomainFollowsPolicyWhenKnownProxyBlockingIsDisabled() {
        val decision = evaluator(proxyRules = setOf(DomainName.from("proxy.example"))).evaluateDomain(
            domain = DomainName.from("login.proxy.example"),
            policy = basePolicy(blockKnownProxyDomains = false),
        )

        assertDecision(
            expectedAction = TrafficAction.ALLOW,
            expectedPolicyDecision = PolicyDecision.Allow,
            expectedReason = PolicyDecisionReason.NO_MATCH,
            expectedShouldCreateEvent = false,
            actual = decision,
        )
    }

    @Test
    fun blocklistWinsOverClassificationAndAllowlist() {
        val policy = basePolicy(
            allowedDomains = setOf(DomainName.from("example.com")),
            blockedDomains = setOf(DomainName.from("proxy.example.com")),
            blockKnownProxyDomains = true,
        )

        val decision = evaluator(proxyRules = setOf(DomainName.from("example.com"))).evaluateDomain(
            domain = DomainName.from("proxy.example.com"),
            policy = policy,
        )

        assertDecision(
            expectedAction = TrafficAction.BLOCK,
            expectedPolicyDecision = PolicyDecision.Block,
            expectedReason = PolicyDecisionReason.BLOCKLIST_MATCH,
            expectedShouldCreateEvent = true,
            actual = decision,
        )
    }

    @Test
    fun unknownDomainStandardBehaviorMatchesPolicyEngine() {
        val decision = evaluator().evaluateDomain(
            domain = DomainName.from("unknown.example"),
            policy = basePolicy(mode = LockdownMode.STANDARD, blockUnknownDomains = false),
        )

        assertDecision(
            expectedAction = TrafficAction.ALLOW,
            expectedPolicyDecision = PolicyDecision.Allow,
            expectedReason = PolicyDecisionReason.NO_MATCH,
            expectedShouldCreateEvent = false,
            actual = decision,
        )
    }

    @Test
    fun monitorOnlyAlertOnlyBehaviorIsPreserved() {
        val decision = evaluator().evaluateDomain(
            domain = DomainName.from("unknown.example"),
            policy = basePolicy(mode = LockdownMode.MONITOR_ONLY, blockUnknownDomains = false),
        )

        assertDecision(
            expectedAction = TrafficAction.ALERT_ONLY,
            expectedPolicyDecision = PolicyDecision.AlertOnly,
            expectedReason = PolicyDecisionReason.NO_MATCH,
            expectedShouldCreateEvent = true,
            actual = decision,
        )
    }

    @Test
    fun crisisLockdownBlocksUnknownDomain() {
        val decision = evaluator().evaluateDomain(
            domain = DomainName.from("unknown.example"),
            policy = basePolicy(mode = LockdownMode.CRISIS_LOCKDOWN, blockUnknownDomains = false),
        )

        assertDecision(
            expectedAction = TrafficAction.BLOCK,
            expectedPolicyDecision = PolicyDecision.Block,
            expectedReason = PolicyDecisionReason.LOCKDOWN_MODE,
            expectedShouldCreateEvent = true,
            actual = decision,
        )
    }

    @Test
    fun engineSourceImportsClassifierAndPolicyButNotAndroidOrVpnService() {
        val sourceRoot = repositoryRoot().resolve("vpn/engine/src/main/kotlin")
        val androidPackage = "android" + "."
        val vpnServicePackage = listOf("com", "vordain", "guard", "vpn", "service").joinToString(".")

        assertSourceTreeContains(sourceRoot, "com.vordain.guard.vpn.classifier")
        assertSourceTreeContains(sourceRoot, "com.vordain.guard.core.policy")
        assertSourceTreeDoesNotContain(sourceRoot, androidPackage)
        assertSourceTreeDoesNotContain(sourceRoot, vpnServicePackage)
    }

    private fun evaluator(
        proxyRules: Set<DomainName> = emptySet(),
    ): DefaultDomainTrafficEvaluator {
        return DefaultDomainTrafficEvaluator(
            domainClassifier = StaticRuleListClassifier(proxyRules),
            policyEngine = DefaultPolicyEngine(),
        )
    }

    private fun basePolicy(
        mode: LockdownMode = LockdownMode.STANDARD,
        allowedDomains: Set<DomainName> = setOf(DomainName.from("school.example")),
        blockedDomains: Set<DomainName> = setOf(DomainName.from("blocked.example")),
        blockUnknownDomains: Boolean = false,
        blockKnownProxyDomains: Boolean = true,
    ): Policy {
        return Policy(
            id = PolicyId("policy-test"),
            mode = mode,
            allowedDomains = allowedDomains,
            blockedDomains = blockedDomains,
            allowedPackages = setOf(AppPackageName("com.school.app")),
            blockedPackages = setOf(AppPackageName("com.blocked.app")),
            blockUnknownDomains = blockUnknownDomains,
            blockKnownProxyDomains = blockKnownProxyDomains,
        )
    }

    private fun assertDecision(
        expectedAction: TrafficAction,
        expectedPolicyDecision: PolicyDecision,
        expectedReason: PolicyDecisionReason,
        expectedShouldCreateEvent: Boolean,
        actual: TrafficDecision,
    ) {
        assertEquals(expectedAction, actual.action)
        assertEquals(
            expected = PolicyEvaluation(
                decision = expectedPolicyDecision,
                reason = expectedReason,
                shouldCreateEvent = expectedShouldCreateEvent,
            ),
            actual = actual.evaluation,
        )
    }

    private fun assertSourceTreeContains(sourceRoot: File, requiredText: String) {
        val kotlinFiles = kotlinFilesUnder(sourceRoot)
        assertTrue(
            actual = kotlinFiles.any { file -> file.readText().contains(requiredText) },
            message = "Expected source under ${sourceRoot.path} to contain $requiredText",
        )
    }

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
}
