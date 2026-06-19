package com.vordain.guard.core.policysync

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.policy.Policy
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignedPolicySnapshotRestorerTest {
    @Test
    fun persistedSignedPayloadRestoresValidPolicy() {
        val restored = restorer.restore(snapshot(update()), currentTimeMillis = 2_000L)

        assertTrue(restored.accepted)
        assertNotNull(restored.policy)
        assertEquals(PolicyVersion("debug-2"), restored.policyVersion)
        assertEquals("debug-debug-2", restored.decodedUpdateId)
    }

    @Test
    fun restoreRejectsMalformedPayload() {
        val restored = restorer.restore(
            PersistedSignedPolicySnapshot(
                encodedPayload = "bad",
                appliedAtMillis = 2_000L,
                expectedDeviceId = childDeviceId,
                lastKnownPolicyVersion = null,
            ),
            currentTimeMillis = 2_000L,
        )

        assertRejected(restored, PolicyUpdateVerificationResult.Malformed)
    }

    @Test
    fun restoreRejectsWrongDevice() {
        val restored = restorer.restore(snapshot(update(), expectedDeviceId = DeviceId("other-child")), currentTimeMillis = 2_000L)

        assertRejected(restored, PolicyUpdateVerificationResult.WrongDevice)
    }

    @Test
    fun restoreRejectsExpiredPayload() {
        val restored = restorer.restore(snapshot(update(expiresAtMillis = 2_000L)), currentTimeMillis = 2_000L)

        assertRejected(restored, PolicyUpdateVerificationResult.Expired)
    }

    @Test
    fun restoreRejectsNotYetValidPayload() {
        val restored = restorer.restore(snapshot(update(issuedAtMillis = 3_000L, expiresAtMillis = 4_000L)), currentTimeMillis = 2_000L)

        assertRejected(restored, PolicyUpdateVerificationResult.NotYetValid)
    }

    @Test
    fun restoreRejectsBlankSignature() {
        val restored = restorer.restore(snapshot(update(signature = PolicyUpdateSignature(""))), currentTimeMillis = 2_000L)

        assertRejected(restored, PolicyUpdateVerificationResult.Malformed)
    }

    @Test
    fun restoreUsesDecodedPayloadAsAuthority() {
        val restored = restorer.restore(
            snapshot(update(policyVersion = PolicyVersion("signed-version")), lastKnownPolicyVersion = PolicyVersion("summary-version")),
            currentTimeMillis = 2_000L,
        )

        assertTrue(restored.accepted)
        assertEquals(PolicyVersion("signed-version"), restored.policyVersion)
    }

    @Test
    fun sourceHasExpectedBoundaries() {
        val sourceRoot = repositoryRoot().resolve("core/policy-sync/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, "storage")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, "browsing " + "history")
        assertSourceTreeDoesNotContain(sourceRoot, "traffic " + "log")
        assertSourceTreeDoesNotContain(sourceRoot, "screen" + "shot")
        assertSourceTreeDoesNotContain(sourceRoot, "message " + "content")
        assertSourceTreeDoesNotContain(sourceRoot, "child " + "activity")
        assertSourceTreeDoesNotContain(sourceRoot, "passive " + "telemetry")
    }

    private fun assertRejected(
        restored: RestoredPolicyEvaluation,
        reason: PolicyUpdateVerificationResult,
    ) {
        assertFalse(restored.accepted)
        assertNull(restored.policy)
        assertNull(restored.policyVersion)
        assertEquals(reason, restored.reason)
    }

    private fun snapshot(
        update: SignedPolicyUpdate,
        expectedDeviceId: DeviceId = childDeviceId,
        lastKnownPolicyVersion: PolicyVersion? = update.policyVersion,
    ): PersistedSignedPolicySnapshot {
        return PersistedSignedPolicySnapshot(
            encodedPayload = codec.encode(update),
            appliedAtMillis = 2_000L,
            expectedDeviceId = expectedDeviceId,
            lastKnownPolicyVersion = lastKnownPolicyVersion,
        )
    }

    private fun update(
        policyVersion: PolicyVersion = PolicyVersion("debug-2"),
        issuedAtMillis: Long = 1_000L,
        expiresAtMillis: Long = 9_999L,
        signature: PolicyUpdateSignature = PolicyUpdateSignature("debug-signature"),
    ): SignedPolicyUpdate {
        return SignedPolicyUpdate(
            updateId = "debug-${policyVersion.value}",
            targetDeviceId = childDeviceId,
            policyVersion = policyVersion,
            issuedAtMillis = issuedAtMillis,
            expiresAtMillis = expiresAtMillis,
            signature = signature,
            policy = Policy(
                id = PolicyId("debug-${policyVersion.value}"),
                mode = LockdownMode.STANDARD,
                allowedDomains = setOf(DomainName.from("school.edu")),
                blockedDomains = setOf(DomainName.from("proxy.example")),
                allowedPackages = emptySet(),
                blockedPackages = emptySet(),
                blockUnknownDomains = false,
                blockKnownProxyDomains = true,
            ),
        )
    }

    private fun assertSourceTreeDoesNotContain(sourceRoot: File, forbiddenText: String) {
        val filesWithForbiddenText = sourceRoot.walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .filter { file -> file.readText().contains(forbiddenText) }
            .toList()

        assertTrue(
            actual = filesWithForbiddenText.isEmpty(),
            message = "Forbidden text $forbiddenText found in ${filesWithForbiddenText.map { it.path }}",
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

    private companion object {
        val childDeviceId = DeviceId("child-debug-device")
        val codec = DebugPolicyUpdateCodec()
        val restorer = SignedPolicySnapshotRestorer()
    }
}
