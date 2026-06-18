package com.vordain.guard.core.policy

import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolicyPatchApplierTest {
    @Test
    fun policyPatchAddsAllowedDomain() {
        val patched = applier.apply(
            policy = policy(),
            patch = PolicyPatch(PolicyPatchOperation.ADD_ALLOWED_DOMAIN, DomainName.from("school.example")),
        )

        assertTrue(DomainName.from("school.example") in patched.allowedDomains)
    }

    @Test
    fun policyPatchAddsBlockedDomain() {
        val patched = applier.apply(
            policy = policy(),
            patch = PolicyPatch(PolicyPatchOperation.ADD_BLOCKED_DOMAIN, DomainName.from("blocked.example")),
        )

        assertTrue(DomainName.from("blocked.example") in patched.blockedDomains)
    }

    @Test
    fun policyPatchRemovesAllowedDomain() {
        val patched = applier.apply(
            policy = policy(allowedDomains = setOf(DomainName.from("school.example"))),
            patch = PolicyPatch(PolicyPatchOperation.REMOVE_ALLOWED_DOMAIN, DomainName.from("school.example")),
        )

        assertFalse(DomainName.from("school.example") in patched.allowedDomains)
    }

    @Test
    fun policyPatchRemovesBlockedDomain() {
        val patched = applier.apply(
            policy = policy(blockedDomains = setOf(DomainName.from("blocked.example"))),
            patch = PolicyPatch(PolicyPatchOperation.REMOVE_BLOCKED_DOMAIN, DomainName.from("blocked.example")),
        )

        assertFalse(DomainName.from("blocked.example") in patched.blockedDomains)
    }

    @Test
    fun applyingBlockedDomainStillCausesBlocklistWin() {
        val patched = applier.apply(
            policy = policy(allowedDomains = setOf(DomainName.from("example.com"))),
            patch = PolicyPatch(PolicyPatchOperation.ADD_BLOCKED_DOMAIN, DomainName.from("example.com")),
        )

        val evaluation = DefaultPolicyEngine().evaluateDomain(DomainName.from("example.com"), patched)

        assertEquals(PolicyDecision.Block, evaluation.decision)
        assertEquals(PolicyDecisionReason.BLOCKLIST_MATCH, evaluation.reason)
    }

    @Test
    fun applyingAllowedDomainAllowsWhenNoStrongerBlockExists() {
        val patched = applier.apply(
            policy = policy(blockUnknownDomains = true),
            patch = PolicyPatch(PolicyPatchOperation.ADD_ALLOWED_DOMAIN, DomainName.from("school.example")),
        )

        val evaluation = DefaultPolicyEngine().evaluateDomain(DomainName.from("school.example"), patched)

        assertEquals(PolicyDecision.Allow, evaluation.decision)
        assertEquals(PolicyDecisionReason.ALLOWLIST_MATCH, evaluation.reason)
    }

    @Test
    fun policyAndReviewModulesDoNotDependOnEachOther() {
        val root = repositoryRoot()

        assertSourceTreeDoesNotContain(root.resolve("core/policy/src/main/kotlin"), listOf("com", "vordain", "guard", "data", "review").joinToString("."))
        assertSourceTreeDoesNotContain(root.resolve("data/review/src/main/kotlin"), listOf("com", "vordain", "guard", "core", "policy").joinToString("."))
        assertSourceTreeDoesNotContain(root.resolve("core/policy/src/main/kotlin"), "android" + ".")
        assertSourceTreeDoesNotContain(root.resolve("core/policy/src/main/kotlin"), "Manager")
        assertSourceTreeDoesNotContain(root.resolve("core/policy/src/main/kotlin"), "TO" + "DO")
        assertSourceTreeDoesNotContain(root.resolve("core/policy/src/main/kotlin"), "FIX" + "ME")
    }

    private fun policy(
        allowedDomains: Set<DomainName> = emptySet(),
        blockedDomains: Set<DomainName> = emptySet(),
        blockUnknownDomains: Boolean = false,
    ): Policy {
        return Policy(
            id = PolicyId("policy-1"),
            mode = LockdownMode.STANDARD,
            allowedDomains = allowedDomains,
            blockedDomains = blockedDomains,
            allowedPackages = emptySet(),
            blockedPackages = emptySet(),
            blockUnknownDomains = blockUnknownDomains,
            blockKnownProxyDomains = true,
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

    private companion object {
        val applier = PolicyPatchApplier()
    }
}
