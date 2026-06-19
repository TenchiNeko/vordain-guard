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
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DebugPolicyUpdateCodecTest {
    @Test
    fun encodingThenDecodingRoundTripsImportantFields() {
        val decoded = assertIs<DebugPolicyUpdateCodecResult.Decoded>(codec.decode(codec.encode(update())))

        assertEquals(DeviceId("child-debug-device"), decoded.update.targetDeviceId)
        assertEquals(PolicyVersion("debug-2"), decoded.update.policyVersion)
        assertEquals(1_000L, decoded.update.issuedAtMillis)
        assertEquals(9_999L, decoded.update.expiresAtMillis)
        assertEquals(PolicyUpdateSignature("debug-signature"), decoded.update.signature)
        assertEquals(LockdownMode.STANDARD, decoded.update.policy.mode)
        assertTrue(decoded.update.policy.blockKnownProxyDomains)
        assertFalse(decoded.update.policy.blockUnknownDomains)
    }

    @Test
    fun decodeRejectsWrongHeader() {
        val result = codec.decode("WRONG\nsignature=debug-signature")

        assertIs<DebugPolicyUpdateCodecResult.Rejected>(result)
    }

    @Test
    fun decodeRejectsBlankSignature() {
        val result = codec.decode(validPayload().replace("signature=debug-signature", "signature= "))

        assertIs<DebugPolicyUpdateCodecResult.Rejected>(result)
    }

    @Test
    fun decodeRejectsMissingTargetDevice() {
        val result = codec.decode(validPayload().lineSequence().filterNot { it.startsWith("targetDeviceId=") }.joinToString("\n"))

        assertIs<DebugPolicyUpdateCodecResult.Rejected>(result)
    }

    @Test
    fun decodeRejectsMalformedTimes() {
        val result = codec.decode(validPayload().replace("issuedAtMillis=1000", "issuedAtMillis=bad"))

        assertIs<DebugPolicyUpdateCodecResult.Rejected>(result)
    }

    @Test
    fun decodeRejectsMalformedTimingOrder() {
        val result = codec.decode(validPayload().replace("expiresAtMillis=9999", "expiresAtMillis=1000"))

        assertIs<DebugPolicyUpdateCodecResult.Rejected>(result)
    }

    @Test
    fun decodeNormalizesAllowDomains() {
        val decoded = assertIs<DebugPolicyUpdateCodecResult.Decoded>(
            codec.decode(validPayload().replace("allowDomains=school.edu,example.com", "allowDomains=SCHOOL.EDU.,Example.COM")),
        )

        assertEquals(setOf(DomainName.from("school.edu"), DomainName.from("example.com")), decoded.update.policy.allowedDomains)
    }

    @Test
    fun decodeNormalizesBlockDomains() {
        val decoded = assertIs<DebugPolicyUpdateCodecResult.Decoded>(
            codec.decode(validPayload().replace("blockDomains=bad.example,proxy.example", "blockDomains=BAD.EXAMPLE.,Proxy.Example")),
        )

        assertEquals(setOf(DomainName.from("bad.example"), DomainName.from("proxy.example")), decoded.update.policy.blockedDomains)
    }

    @Test
    fun decodeStripsEmptyDomainEntries() {
        val decoded = assertIs<DebugPolicyUpdateCodecResult.Decoded>(
            codec.decode(validPayload().replace("allowDomains=school.edu,example.com", "allowDomains=school.edu,, ,example.com,")),
        )

        assertEquals(setOf(DomainName.from("school.edu"), DomainName.from("example.com")), decoded.update.policy.allowedDomains)
    }

    @Test
    fun decodedUpdateAppliesWithStubVerifier() {
        val decoded = assertIs<DebugPolicyUpdateCodecResult.Decoded>(codec.decode(validPayload()))
        val result = applier.apply(
            update = decoded.update,
            expectedDeviceId = DeviceId("child-debug-device"),
            currentTimeMillis = 2_000L,
        )

        assertTrue(result.accepted)
        assertEquals(PolicyVersion("debug-2"), result.policyVersion)
    }

    @Test
    fun wrongTargetDeviceRejectsThroughApplier() {
        val decoded = assertIs<DebugPolicyUpdateCodecResult.Decoded>(codec.decode(validPayload()))
        val result = applier.apply(
            update = decoded.update,
            expectedDeviceId = DeviceId("other-child"),
            currentTimeMillis = 2_000L,
        )

        assertFalse(result.accepted)
        assertEquals(PolicyUpdateVerificationResult.WrongDevice, result.reason)
    }

    @Test
    fun sourceDoesNotImportForbiddenPackagesOrRestrictedTerms() {
        val sourceRoot = repositoryRoot().resolve("core/policy-sync/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "features").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, "browsing " + "history")
        assertSourceTreeDoesNotContain(sourceRoot, "traffic " + "log")
        assertSourceTreeDoesNotContain(sourceRoot, "screen" + "shot")
        assertSourceTreeDoesNotContain(sourceRoot, "message " + "content")
        assertSourceTreeDoesNotContain(sourceRoot, "child " + "activity")
        assertSourceTreeDoesNotContain(sourceRoot, "passive " + "telemetry")
        assertSourceTreeDoesNotContain(sourceRoot, "relay " + "payload")
        assertSourceTreeDoesNotContain(sourceRoot, "Manager")
        assertSourceTreeDoesNotContain(sourceRoot, "TO" + "DO")
        assertSourceTreeDoesNotContain(sourceRoot, "FIX" + "ME")
    }

    private fun validPayload(): String {
        return codec.encode(update())
    }

    private fun update(): SignedPolicyUpdate {
        return SignedPolicyUpdate(
            updateId = "debug-debug-2",
            targetDeviceId = DeviceId("child-debug-device"),
            policyVersion = PolicyVersion("debug-2"),
            issuedAtMillis = 1_000L,
            expiresAtMillis = 9_999L,
            signature = PolicyUpdateSignature("debug-signature"),
            policy = Policy(
                id = PolicyId("debug-debug-2"),
                mode = LockdownMode.STANDARD,
                allowedDomains = setOf(DomainName.from("school.edu"), DomainName.from("example.com")),
                blockedDomains = setOf(DomainName.from("bad.example"), DomainName.from("proxy.example")),
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
        val codec = DebugPolicyUpdateCodec()
        val applier = PolicyUpdateApplier(StubPolicyUpdateVerifier())
    }
}
