package com.vordain.guard.data.relay

import com.vordain.guard.core.crypto.EncryptedPayload
import com.vordain.guard.core.model.DeviceId
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemoryRelayClientTest {
    @Test
    fun relayMessageStoresDeviceIdsEncryptedPayloadAndCreatedTime() {
        val message = relayMessage("message-1")

        assertEquals("message-1", message.messageId)
        assertEquals(DeviceId("child-device"), message.sourceDeviceId)
        assertEquals(DeviceId("parent-device"), message.targetDeviceId)
        assertEquals("test-encrypted-payload", message.payload.algorithm)
        assertContentEquals(byteArrayOf(1, 2, 3), message.payload.ciphertext)
        assertContentEquals(byteArrayOf(4, 5, 6), message.payload.nonce)
        assertEquals(1_234L, message.createdAtMillis)
    }

    @Test
    fun clientSendsAndStoresEncryptedMessages() {
        val client = InMemoryRelayClient()
        val message = relayMessage("message-1")

        val result = client.send(message)

        assertEquals(RelaySendResult.Success, result)
        assertEquals(listOf("message-1"), client.sent().map { it.messageId })
    }

    @Test
    fun clientPreservesSendOrder() {
        val client = InMemoryRelayClient()

        client.send(relayMessage("message-1"))
        client.send(relayMessage("message-2"))
        client.send(relayMessage("message-3"))

        assertEquals(listOf("message-1", "message-2", "message-3"), client.sent().map { it.messageId })
    }

    @Test
    fun clientCanReturnSuccess() {
        val result = InMemoryRelayClient(RelaySendResult.Success).send(relayMessage("message-1"))

        assertEquals(RelaySendResult.Success, result)
    }

    @Test
    fun clientCanReturnRetryableFailure() {
        val client = InMemoryRelayClient(RelaySendResult.RetryableFailure("temporary relay outage"))

        val result = client.send(relayMessage("message-1"))

        assertEquals(RelaySendResult.RetryableFailure("temporary relay outage"), result)
        assertTrue(client.sent().isEmpty())
    }

    @Test
    fun clientCanReturnPermanentFailure() {
        val client = InMemoryRelayClient(RelaySendResult.PermanentFailure("target device not paired"))

        val result = client.send(relayMessage("message-1"))

        assertEquals(RelaySendResult.PermanentFailure("target device not paired"), result)
        assertTrue(client.sent().isEmpty())
    }

    @Test
    fun clientDoesNotExposeMutablePayloadReferences() {
        val ciphertext = byteArrayOf(1, 2, 3)
        val nonce = byteArrayOf(4, 5, 6)
        val client = InMemoryRelayClient()

        client.send(
            relayMessage(
                id = "message-1",
                payload = EncryptedPayload(
                    algorithm = "test-encrypted-payload",
                    ciphertext = ciphertext,
                    nonce = nonce,
                ),
            ),
        )
        ciphertext[0] = 9
        nonce[0] = 9

        val storedPayload = client.sent().single().payload
        assertContentEquals(byteArrayOf(1, 2, 3), storedPayload.ciphertext)
        assertContentEquals(byteArrayOf(4, 5, 6), storedPayload.nonce)
    }

    @Test
    fun relaySourceDoesNotImportReadableEventOrForbiddenPackages() {
        val sourceRoot = repositoryRoot().resolve("data/relay/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "Security" + "Event")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "core", "events").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "features").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data", "local").joinToString("."))
    }

    @Test
    fun relayClientInterfaceDoesNotMentionReadableEventType() {
        val source = repositoryRoot()
            .resolve("data/relay/src/main/kotlin/com/vordain/guard/data/relay/RelayClient.kt")
            .readText()

        assertTrue(!source.contains("Security" + "Event"))
    }

    private fun relayMessage(
        id: String,
        payload: EncryptedPayload = encryptedPayload(),
    ): RelayMessage {
        return RelayMessage(
            messageId = id,
            sourceDeviceId = DeviceId("child-device"),
            targetDeviceId = DeviceId("parent-device"),
            payload = payload,
            createdAtMillis = 1_234L,
        )
    }

    private fun encryptedPayload(): EncryptedPayload {
        return EncryptedPayload(
            algorithm = "test-encrypted-payload",
            ciphertext = byteArrayOf(1, 2, 3),
            nonce = byteArrayOf(4, 5, 6),
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
