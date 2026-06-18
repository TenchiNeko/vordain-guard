package com.vordain.guard.core.policy

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultPolicyEngineTest {
    private val engine = DefaultPolicyEngine()

    @Test
    fun blocklistedDomainIsBlocked() {
        val evaluation = engine.evaluateDomain(DomainName.from("proxy.example"), basePolicy())

        assertEvaluation(
            expectedDecision = PolicyDecision.Block,
            expectedReason = PolicyDecisionReason.BLOCKLIST_MATCH,
            expectedShouldCreateEvent = true,
            actual = evaluation,
        )
    }

    @Test
    fun allowlistedDomainIsAllowed() {
        val evaluation = engine.evaluateDomain(DomainName.from("school.example"), basePolicy())

        assertEvaluation(
            expectedDecision = PolicyDecision.Allow,
            expectedReason = PolicyDecisionReason.ALLOWLIST_MATCH,
            expectedShouldCreateEvent = false,
            actual = evaluation,
        )
    }

    @Test
    fun unknownDomainIsBlockedWhenUnknownDomainsAreBlocked() {
        val evaluation = engine.evaluateDomain(
            domain = DomainName.from("unknown.example"),
            policy = basePolicy(blockUnknownDomains = true),
        )

        assertEvaluation(
            expectedDecision = PolicyDecision.Block,
            expectedReason = PolicyDecisionReason.UNKNOWN_DOMAIN_BLOCKED,
            expectedShouldCreateEvent = true,
            actual = evaluation,
        )
    }

    @Test
    fun unknownDomainIsAllowedInStandardModeWhenUnknownDomainsAreNotBlocked() {
        val evaluation = engine.evaluateDomain(
            domain = DomainName.from("unknown.example"),
            policy = basePolicy(mode = LockdownMode.STANDARD, blockUnknownDomains = false),
        )

        assertEvaluation(
            expectedDecision = PolicyDecision.Allow,
            expectedReason = PolicyDecisionReason.NO_MATCH,
            expectedShouldCreateEvent = false,
            actual = evaluation,
        )
    }

    @Test
    fun unknownDomainIsAlertOnlyInMonitorModeWhenUnknownDomainsAreNotBlocked() {
        val evaluation = engine.evaluateDomain(
            domain = DomainName.from("unknown.example"),
            policy = basePolicy(mode = LockdownMode.MONITOR_ONLY, blockUnknownDomains = false),
        )

        assertEvaluation(
            expectedDecision = PolicyDecision.AlertOnly,
            expectedReason = PolicyDecisionReason.NO_MATCH,
            expectedShouldCreateEvent = true,
            actual = evaluation,
        )
    }

    @Test
    fun crisisLockdownBlocksUnknownDomainWhenUnknownDomainsAreNotBlocked() {
        val evaluation = engine.evaluateDomain(
            domain = DomainName.from("unknown.example"),
            policy = basePolicy(mode = LockdownMode.CRISIS_LOCKDOWN, blockUnknownDomains = false),
        )

        assertEvaluation(
            expectedDecision = PolicyDecision.Block,
            expectedReason = PolicyDecisionReason.LOCKDOWN_MODE,
            expectedShouldCreateEvent = true,
            actual = evaluation,
        )
    }

    @Test
    fun blocklistedAppPackageIsBlocked() {
        val evaluation = engine.evaluateApp(AppPackageName("com.proxy.app"), basePolicy())

        assertEvaluation(
            expectedDecision = PolicyDecision.Block,
            expectedReason = PolicyDecisionReason.BLOCKLIST_MATCH,
            expectedShouldCreateEvent = true,
            actual = evaluation,
        )
    }

    @Test
    fun allowedAppPackageIsAllowed() {
        val evaluation = engine.evaluateApp(AppPackageName("com.school.app"), basePolicy())

        assertEvaluation(
            expectedDecision = PolicyDecision.Allow,
            expectedReason = PolicyDecisionReason.ALLOWLIST_MATCH,
            expectedShouldCreateEvent = false,
            actual = evaluation,
        )
    }

    @Test
    fun crisisLockdownBlocksUnknownAppPackage() {
        val evaluation = engine.evaluateApp(
            packageName = AppPackageName("com.unknown.app"),
            policy = basePolicy(mode = LockdownMode.CRISIS_LOCKDOWN),
        )

        assertEvaluation(
            expectedDecision = PolicyDecision.Block,
            expectedReason = PolicyDecisionReason.LOCKDOWN_MODE,
            expectedShouldCreateEvent = true,
            actual = evaluation,
        )
    }

    @Test
    fun blankDomainIsRejectedBeforePolicyEvaluation() {
        assertFailsWith<IllegalArgumentException> {
            DomainName.from("   ")
        }
    }

    @Test
    fun invalidAppPackageReturnsInvalidInputReason() {
        val evaluation = engine.evaluateApp(AppPackageName(""), basePolicy())

        assertEvaluation(
            expectedDecision = PolicyDecision.Block,
            expectedReason = PolicyDecisionReason.INVALID_INPUT,
            expectedShouldCreateEvent = true,
            actual = evaluation,
        )
    }

    @Test
    fun domainNormalizationMatchesPolicyEntries() {
        val evaluation = engine.evaluateDomain(DomainName.from("  SCHOOL.EXAMPLE. "), basePolicy())

        assertEvaluation(
            expectedDecision = PolicyDecision.Allow,
            expectedReason = PolicyDecisionReason.ALLOWLIST_MATCH,
            expectedShouldCreateEvent = false,
            actual = evaluation,
        )
    }

    @Test
    fun normalizedDomainMatchesBlockedPolicyEntry() {
        val policy = basePolicy(
            blockedDomains = setOf(DomainName.from("example.com")),
        )

        val evaluation = engine.evaluateDomain(DomainName.from("EXAMPLE.COM."), policy)

        assertEvaluation(
            expectedDecision = PolicyDecision.Block,
            expectedReason = PolicyDecisionReason.BLOCKLIST_MATCH,
            expectedShouldCreateEvent = true,
            actual = evaluation,
        )
    }

    @Test
    fun blocklistedDomainBlocksSubdomain() {
        val policy = basePolicy(
            blockedDomains = setOf(DomainName.from("example.com")),
        )

        val evaluation = engine.evaluateDomain(DomainName.from("login.example.com"), policy)

        assertEvaluation(
            expectedDecision = PolicyDecision.Block,
            expectedReason = PolicyDecisionReason.BLOCKLIST_MATCH,
            expectedShouldCreateEvent = true,
            actual = evaluation,
        )
    }

    @Test
    fun allowlistedDomainAllowsSubdomain() {
        val policy = basePolicy(
            allowedDomains = setOf(DomainName.from("school.edu")),
        )

        val evaluation = engine.evaluateDomain(DomainName.from("login.school.edu"), policy)

        assertEvaluation(
            expectedDecision = PolicyDecision.Allow,
            expectedReason = PolicyDecisionReason.ALLOWLIST_MATCH,
            expectedShouldCreateEvent = false,
            actual = evaluation,
        )
    }

    @Test
    fun blocklistWinsWhenDomainMatchesBothAllowlistAndBlocklist() {
        val policy = basePolicy(
            allowedDomains = setOf(DomainName.from("example.com")),
            blockedDomains = setOf(DomainName.from("login.example.com")),
        )

        val evaluation = engine.evaluateDomain(DomainName.from("login.example.com"), policy)

        assertEvaluation(
            expectedDecision = PolicyDecision.Block,
            expectedReason = PolicyDecisionReason.BLOCKLIST_MATCH,
            expectedShouldCreateEvent = true,
            actual = evaluation,
        )
    }

    @Test
    fun blocklistWinsWhenDomainSubdomainMatchesBothAllowlistAndBlocklist() {
        val policy = basePolicy(
            allowedDomains = setOf(DomainName.from("example.com")),
            blockedDomains = setOf(DomainName.from("example.com")),
        )

        val evaluation = engine.evaluateDomain(DomainName.from("deep.login.example.com"), policy)

        assertEvaluation(
            expectedDecision = PolicyDecision.Block,
            expectedReason = PolicyDecisionReason.BLOCKLIST_MATCH,
            expectedShouldCreateEvent = true,
            actual = evaluation,
        )
    }

    @Test
    fun corePolicyImplementationDoesNotImportAndroidPackages() {
        assertSourceTreeDoesNotContainAndroidImports(sourceRoot = repositoryRoot().resolve("core/policy/src/main/kotlin"))
    }

    @Test
    fun corePolicyTestsDoNotImportAndroidPackages() {
        assertSourceTreeDoesNotContainAndroidImports(sourceRoot = repositoryRoot().resolve("core/policy/src/test/kotlin"))
    }

    private fun basePolicy(
        mode: LockdownMode = LockdownMode.STANDARD,
        blockUnknownDomains: Boolean = false,
        allowedDomains: Set<DomainName> = setOf(DomainName.from("school.example")),
        blockedDomains: Set<DomainName> = setOf(DomainName.from("proxy.example")),
    ): Policy {
        return Policy(
            id = PolicyId("policy-test"),
            mode = mode,
            allowedDomains = allowedDomains,
            blockedDomains = blockedDomains,
            allowedPackages = setOf(AppPackageName("com.school.app")),
            blockedPackages = setOf(AppPackageName("com.proxy.app")),
            blockUnknownDomains = blockUnknownDomains,
            blockKnownProxyDomains = true,
        )
    }

    private fun assertEvaluation(
        expectedDecision: PolicyDecision,
        expectedReason: PolicyDecisionReason,
        expectedShouldCreateEvent: Boolean,
        actual: PolicyEvaluation,
    ) {
        assertEquals(
            expected = PolicyEvaluation(
                decision = expectedDecision,
                reason = expectedReason,
                shouldCreateEvent = expectedShouldCreateEvent,
            ),
            actual = actual,
        )
    }

    private fun assertSourceTreeDoesNotContainAndroidImports(sourceRoot: File) {
        val androidPackagePrefix = "android" + "."
        val kotlinFiles = sourceRoot.walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .toList()

        assertTrue(kotlinFiles.isNotEmpty(), "Expected Kotlin source files under ${sourceRoot.path}")

        val filesWithAndroidImports = kotlinFiles.filter { file ->
            file.readLines().any { line -> line.trim().startsWith("import $androidPackagePrefix") }
        }

        assertTrue(
            actual = filesWithAndroidImports.isEmpty(),
            message = "Android imports are not allowed in core/policy: ${filesWithAndroidImports.map { it.path }}",
        )
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
