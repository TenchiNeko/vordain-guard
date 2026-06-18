package com.vordain.guard.data.heartbeat

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.model.ProtectionState
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemoryHeartbeatClientTest {
    @Test
    fun heartbeatPreservesMinimalProtectionMetadata() {
        val heartbeat = heartbeat(sequenceNumber = 42L)

        assertEquals(DeviceId("child-device"), heartbeat.deviceId)
        assertEquals(1_234L, heartbeat.reportedAtMillis)
        assertEquals(42L, heartbeat.sequenceNumber)
        assertEquals(ProtectionState.DEGRADED, heartbeat.protectionState)
        assertEquals(PolicyId("policy-1"), heartbeat.policyId)
    }

    @Test
    fun clientSendsAndStoresHeartbeats() {
        val client = InMemoryHeartbeatClient()

        val result = client.send(heartbeat(sequenceNumber = 1L))

        assertEquals(HeartbeatSendResult.Success, result)
        assertEquals(listOf(1L), client.sent().map { it.sequenceNumber })
    }

    @Test
    fun clientPreservesSendOrder() {
        val client = InMemoryHeartbeatClient()

        client.send(heartbeat(sequenceNumber = 1L))
        client.send(heartbeat(sequenceNumber = 2L))
        client.send(heartbeat(sequenceNumber = 3L))

        assertEquals(listOf(1L, 2L, 3L), client.sent().map { it.sequenceNumber })
    }

    @Test
    fun clientCanReturnSuccess() {
        val result = InMemoryHeartbeatClient(HeartbeatSendResult.Success).send(heartbeat())

        assertEquals(HeartbeatSendResult.Success, result)
    }

    @Test
    fun clientCanReturnRetryableFailure() {
        val client = InMemoryHeartbeatClient(HeartbeatSendResult.RetryableFailure("temporary unavailable"))

        val result = client.send(heartbeat())

        assertEquals(HeartbeatSendResult.RetryableFailure("temporary unavailable"), result)
        assertTrue(client.sent().isEmpty())
    }

    @Test
    fun clientCanReturnPermanentFailure() {
        val client = InMemoryHeartbeatClient(HeartbeatSendResult.PermanentFailure("device not registered"))

        val result = client.send(heartbeat())

        assertEquals(HeartbeatSendResult.PermanentFailure("device not registered"), result)
        assertTrue(client.sent().isEmpty())
    }

    @Test
    fun clientDoesNotExposeMutableInternalList() {
        val client = InMemoryHeartbeatClient()
        client.send(heartbeat(sequenceNumber = 1L))

        val firstRead = client.sent()
        firstRead.toMutableList().clear()

        assertEquals(listOf(1L), client.sent().map { it.sequenceNumber })
    }

    @Test
    fun sourceDoesNotImportForbiddenPackagesOrActivityMetadata() {
        val sourceRoot = repositoryRoot().resolve("data/heartbeat/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "core", "events").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "features").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data", "relay").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data", "outbox").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("Domain", "Name").joinToString(""))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("App", "Package", "Name").joinToString(""))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("Security", "Event").joinToString(""))
        assertSourceTreeDoesNotContain(sourceRoot, "screen" + "shot")
        assertSourceTreeDoesNotContain(sourceRoot, "brows" + "ing")
        assertSourceTreeDoesNotContain(sourceRoot, "u" + "rl")
        assertSourceTreeDoesNotContain(sourceRoot, "U" + "RL")
    }

    private fun heartbeat(sequenceNumber: Long = 7L): ProtectionHeartbeat {
        return ProtectionHeartbeat(
            deviceId = DeviceId("child-device"),
            reportedAtMillis = 1_234L,
            sequenceNumber = sequenceNumber,
            protectionState = ProtectionState.DEGRADED,
            policyId = PolicyId("policy-1"),
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
