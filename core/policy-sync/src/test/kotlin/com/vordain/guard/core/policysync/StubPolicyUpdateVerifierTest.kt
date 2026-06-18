package com.vordain.guard.core.policysync

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.policy.Policy
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StubPolicyUpdateVerifierTest {
    @Test
    fun validSignedPolicyUpdateVerifiesValid() {
        val result = verifier.verify(
            update = update(),
            expectedDeviceId = childDeviceId,
            currentTimeMillis = 1_500L,
        )

        assertEquals(PolicyUpdateVerificationResult.Valid, result)
    }

    @Test
    fun validUpdateAppliesAndReturnsPolicyVersion() {
        val update = update(policyVersion = PolicyVersion("policy-v2"))

        val result = applier.apply(
            update = update,
            expectedDeviceId = childDeviceId,
            currentTimeMillis = 1_500L,
        )

        assertTrue(result.accepted)
        assertEquals(update.policy, result.policy)
        assertEquals(PolicyVersion("policy-v2"), result.policyVersion)
        assertEquals(PolicyUpdateVerificationResult.Valid, result.reason)
    }

    @Test
    fun expiredUpdateIsRejected() {
        val result = verifier.verify(
            update = update(expiresAtMillis = 2_000L),
            expectedDeviceId = childDeviceId,
            currentTimeMillis = 2_000L,
        )

        assertEquals(PolicyUpdateVerificationResult.Expired, result)
    }

    @Test
    fun notYetValidUpdateIsRejected() {
        val result = verifier.verify(
            update = update(issuedAtMillis = 2_000L, expiresAtMillis = 3_000L),
            expectedDeviceId = childDeviceId,
            currentTimeMillis = 1_999L,
        )

        assertEquals(PolicyUpdateVerificationResult.NotYetValid, result)
    }

    @Test
    fun wrongTargetDeviceIsRejected() {
        val result = verifier.verify(
            update = update(targetDeviceId = DeviceId("other-child")),
            expectedDeviceId = childDeviceId,
            currentTimeMillis = 1_500L,
        )

        assertEquals(PolicyUpdateVerificationResult.WrongDevice, result)
    }

    @Test
    fun blankSignatureIsRejected() {
        val result = verifier.verify(
            update = update(signature = PolicyUpdateSignature(" ")),
            expectedDeviceId = childDeviceId,
            currentTimeMillis = 1_500L,
        )

        assertEquals(PolicyUpdateVerificationResult.InvalidSignature, result)
    }

    @Test
    fun malformedTimingIsRejected() {
        val result = verifier.verify(
            update = update(issuedAtMillis = 2_000L, expiresAtMillis = 2_000L),
            expectedDeviceId = childDeviceId,
            currentTimeMillis = 2_000L,
        )

        assertEquals(PolicyUpdateVerificationResult.Malformed, result)
    }

    @Test
    fun exactExpiryBoundaryIsExpired() {
        val result = verifier.verify(
            update = update(issuedAtMillis = 1_000L, expiresAtMillis = 2_000L),
            expectedDeviceId = childDeviceId,
            currentTimeMillis = 2_000L,
        )

        assertEquals(PolicyUpdateVerificationResult.Expired, result)
    }

    @Test
    fun applierDoesNotReturnPolicyForRejectedUpdate() {
        val result = applier.apply(
            update = update(signature = PolicyUpdateSignature("")),
            expectedDeviceId = childDeviceId,
            currentTimeMillis = 1_500L,
        )

        assertFalse(result.accepted)
        assertNull(result.policy)
        assertNull(result.policyVersion)
        assertEquals(PolicyUpdateVerificationResult.InvalidSignature, result.reason)
    }

    @Test
    fun sourceDoesNotImportForbiddenPackagesOrTelemetryTerms() {
        val sourceRoot = repositoryRoot().resolve("core/policy-sync/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "features").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("Security", "Event").joinToString(""))
        assertSourceTreeDoesNotContain(sourceRoot, "browsing " + "history")
        assertSourceTreeDoesNotContain(sourceRoot, "traffic " + "log")
        assertSourceTreeDoesNotContain(sourceRoot, "screen" + "shot")
        assertSourceTreeDoesNotContain(sourceRoot, "message " + "content")
        assertSourceTreeDoesNotContain(sourceRoot, "child " + "activity")
        assertSourceTreeDoesNotContain(sourceRoot, "Manager")
        assertSourceTreeDoesNotContain(sourceRoot, "TO" + "DO")
        assertSourceTreeDoesNotContain(sourceRoot, "FIX" + "ME")
    }

    private fun update(
        targetDeviceId: DeviceId = childDeviceId,
        policyVersion: PolicyVersion = PolicyVersion("policy-v1"),
        issuedAtMillis: Long = 1_000L,
        expiresAtMillis: Long = 2_000L,
        signature: PolicyUpdateSignature = PolicyUpdateSignature("test-signature"),
    ): SignedPolicyUpdate {
        return SignedPolicyUpdate(
            updateId = "update-1",
            targetDeviceId = targetDeviceId,
            policy = policy(),
            policyVersion = policyVersion,
            issuedAtMillis = issuedAtMillis,
            expiresAtMillis = expiresAtMillis,
            signature = signature,
        )
    }

    private fun policy(): Policy {
        return Policy(
            id = PolicyId("policy-1"),
            mode = LockdownMode.STANDARD,
            allowedDomains = emptySet(),
            blockedDomains = emptySet(),
            allowedPackages = emptySet(),
            blockedPackages = emptySet(),
            blockUnknownDomains = true,
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
        val childDeviceId = DeviceId("child-1")
        val verifier = StubPolicyUpdateVerifier()
        val applier = PolicyUpdateApplier(verifier)
    }
}
