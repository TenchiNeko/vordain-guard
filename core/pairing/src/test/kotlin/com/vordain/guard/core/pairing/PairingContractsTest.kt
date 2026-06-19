package com.vordain.guard.core.pairing

import com.vordain.guard.core.model.DeviceId
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PairingContractsTest {
    @Test
    fun validInviteEvaluatesInviteCreated() {
        val result = evaluator.evaluateInvite(invite(), currentTimeMillis = 2_000L)

        assertEquals(PairingStatus.INVITE_CREATED, result.status)
    }

    @Test
    fun expiredInviteEvaluatesExpired() {
        val result = evaluator.evaluateInvite(invite(), currentTimeMillis = 9_999L)

        assertEquals(PairingStatus.EXPIRED, result.status)
    }

    @Test
    fun malformedInviteTimingIsRejected() {
        val result = evaluator.evaluateInvite(invite(expiresAtMillis = 1_000L), currentTimeMillis = 1_000L)

        assertEquals(PairingStatus.MALFORMED, result.status)
    }

    @Test
    fun validAcceptancePairsDevices() {
        val result = evaluator.evaluateAcceptance(invite(), acceptance(), currentTimeMillis = 2_000L)

        assertEquals(PairingStatus.PAIRED, result.status)
        assertEquals(DeviceId("parent-debug-device"), result.pairedParent?.deviceId)
        assertEquals(DeviceId("child-debug-device"), result.pairedChild?.deviceId)
    }

    @Test
    fun wrongSessionIsRejected() {
        val result = evaluator.evaluateAcceptance(
            invite = invite(),
            acceptance = acceptance(sessionId = PairingSessionId("other-session")),
            currentTimeMillis = 2_000L,
        )

        assertEquals(PairingStatus.REJECTED, result.status)
    }

    @Test
    fun wrongVerificationCodeIsRejected() {
        val result = evaluator.evaluateAcceptance(
            invite = invite(),
            acceptance = acceptance(verificationCode = PairingVerificationCode("654321")),
            currentTimeMillis = 2_000L,
        )

        assertEquals(PairingStatus.REJECTED, result.status)
    }

    @Test
    fun blankFingerprintIsRejected() {
        val result = runCatching { PairingPublicKeyFingerprint(" ") }

        assertTrue(result.isFailure)
    }

    @Test
    fun inviteCodecRoundTrips() {
        val decoded = assertIs<DebugPairingInviteCodecResult.Decoded>(inviteCodec.decode(inviteCodec.encode(invite())))

        assertEquals(PairingSessionId("debug-session-1"), decoded.invite.sessionId)
        assertEquals(DeviceId("parent-debug-device"), decoded.invite.parentDeviceProfile.deviceId)
    }

    @Test
    fun acceptanceCodecRoundTrips() {
        val decoded = assertIs<DebugPairingAcceptanceCodecResult.Decoded>(
            acceptanceCodec.decode(acceptanceCodec.encode(acceptance())),
        )

        assertEquals(PairingSessionId("debug-session-1"), decoded.acceptance.sessionId)
        assertEquals(DeviceId("child-debug-device"), decoded.acceptance.childDeviceProfile.deviceId)
    }

    @Test
    fun inviteCodecRejectsWrongHeader() {
        val result = inviteCodec.decode("WRONG\nsessionId=debug-session-1")

        assertIs<DebugPairingInviteCodecResult.Rejected>(result)
    }

    @Test
    fun acceptanceCodecRejectsWrongHeader() {
        val result = acceptanceCodec.decode("WRONG\nsessionId=debug-session-1")

        assertIs<DebugPairingAcceptanceCodecResult.Rejected>(result)
    }

    @Test
    fun codecRejectsUnknownCapability() {
        val payload = inviteCodec.encode(invite()).replace("PARENT_REVIEW", "UNKNOWN_CAPABILITY")

        assertIs<DebugPairingInviteCodecResult.Rejected>(inviteCodec.decode(payload))
    }

    @Test
    fun sourceHasExpectedBoundaries() {
        val sourceRoot = repositoryRoot().resolve("core/pairing/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "core", "events").joinToString("."))
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

    private fun invite(
        expiresAtMillis: Long = 9_999L,
    ): PairingInvite {
        return PairingInvite(
            sessionId = PairingSessionId("debug-session-1"),
            parentDeviceProfile = PairingDeviceProfile(
                deviceId = DeviceId("parent-debug-device"),
                role = PairingRole.PARENT,
                displayName = "Parent Debug Device",
                publicKeyFingerprint = PairingPublicKeyFingerprint("debug-parent-fingerprint"),
                capabilities = defaultCapabilities,
            ),
            createdAtMillis = 1_000L,
            expiresAtMillis = expiresAtMillis,
            verificationCode = PairingVerificationCode("123456"),
        )
    }

    private fun acceptance(
        sessionId: PairingSessionId = PairingSessionId("debug-session-1"),
        verificationCode: PairingVerificationCode = PairingVerificationCode("123456"),
    ): PairingAcceptance {
        return PairingAcceptance(
            sessionId = sessionId,
            childDeviceProfile = PairingDeviceProfile(
                deviceId = DeviceId("child-debug-device"),
                role = PairingRole.CHILD,
                displayName = "Child Debug Tablet",
                publicKeyFingerprint = PairingPublicKeyFingerprint("debug-child-fingerprint"),
                capabilities = defaultCapabilities,
            ),
            acceptedAtMillis = 2_000L,
            verificationCode = verificationCode,
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
        val evaluator = PairingEvaluator()
        val inviteCodec = DebugPairingInviteCodec()
        val acceptanceCodec = DebugPairingAcceptanceCodec()
        val defaultCapabilities = setOf(
            PairingCapability.POLICY_UPDATES,
            PairingCapability.HEARTBEAT_STATUS,
            PairingCapability.ENCRYPTED_ALERTS,
            PairingCapability.PARENT_REVIEW,
        )
    }
}
