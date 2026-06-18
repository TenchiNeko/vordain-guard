package com.vordain.guard.data.outbox

import com.vordain.guard.core.crypto.EncryptedPayload
import com.vordain.guard.core.events.EventClock
import com.vordain.guard.core.events.EventSeverity
import com.vordain.guard.core.events.SecurityEvent
import com.vordain.guard.core.events.SecurityEventType
import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.data.local.InMemorySecurityEventQueue
import com.vordain.guard.data.local.SecurityEventQueue
import com.vordain.guard.data.relay.RelayClient
import com.vordain.guard.data.relay.RelayMessage
import com.vordain.guard.data.relay.RelaySendResult
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SecurityEventOutboxDispatcherTest {
    @Test
    fun successfulDispatchEncryptsQueuedEventAndSendsRelayMessage() {
        val fixture = fixture()
        fixture.queue.enqueue(securityEvent("event-1"))

        val result = fixture.dispatcher.dispatch(request(limit = 10))

        assertEquals(OutboxDispatchResult(1, 1, 0, 0), result)
        assertEquals(listOf("event-1"), fixture.encryptor.encryptedEventIds)
        assertEquals(listOf("relay-1"), fixture.relay.sentMessages.map { it.messageId })
        assertContentEquals(byteArrayOf(1, 2, 3), fixture.relay.sentMessages.single().payload.ciphertext)
    }

    @Test
    fun successfulDispatchMarksEventDeliveredAndRemovesItFromPending() {
        val fixture = fixture()
        fixture.queue.enqueue(securityEvent("event-1"))

        fixture.dispatcher.dispatch(request(limit = 10))

        assertTrue(fixture.queue.pending(limit = 10).isEmpty())
    }

    @Test
    fun relayMessageContainsSourceAndTargetDeviceIds() {
        val fixture = fixture()
        fixture.queue.enqueue(securityEvent("event-1"))

        fixture.dispatcher.dispatch(request(limit = 10))

        val message = fixture.relay.sentMessages.single()
        assertEquals(DeviceId("child-device"), message.sourceDeviceId)
        assertEquals(DeviceId("parent-device"), message.targetDeviceId)
        assertEquals(5_000L, message.createdAtMillis)
    }

    @Test
    fun relayMessageContainsEncryptedPayloadNotReadableEvent() {
        val fixture = fixture()
        fixture.queue.enqueue(securityEvent("event-1"))

        fixture.dispatcher.dispatch(request(limit = 10))

        val message = fixture.relay.sentMessages.single()
        assertEquals("test-encryption", message.payload.algorithm)
        assertContentEquals(byteArrayOf(1, 2, 3), message.payload.ciphertext)
    }

    @Test
    fun dispatcherRespectsLimit() {
        val fixture = fixture()
        fixture.queue.enqueue(securityEvent("event-1"))
        fixture.queue.enqueue(securityEvent("event-2"))
        fixture.queue.enqueue(securityEvent("event-3"))

        val result = fixture.dispatcher.dispatch(request(limit = 2))

        assertEquals(2, result.attemptedCount)
        assertEquals(listOf("event-1", "event-2"), fixture.encryptor.encryptedEventIds)
        assertEquals(listOf("event-3"), fixture.queue.pending(limit = 10).map { it.event.id })
    }

    @Test
    fun multipleEventsPreserveQueueAndSendOrder() {
        val fixture = fixture()
        fixture.queue.enqueue(securityEvent("event-1"))
        fixture.queue.enqueue(securityEvent("event-2"))
        fixture.queue.enqueue(securityEvent("event-3"))

        fixture.dispatcher.dispatch(request(limit = 10))

        assertEquals(listOf("event-1", "event-2", "event-3"), fixture.encryptor.encryptedEventIds)
        assertEquals(listOf("relay-1", "relay-2", "relay-3"), fixture.relay.sentMessages.map { it.messageId })
    }

    @Test
    fun retryableRelayFailureMarksEventFailedAndLeavesItPending() {
        val fixture = fixture(relayResults = listOf(RelaySendResult.RetryableFailure("temporary failure")))
        fixture.queue.enqueue(securityEvent("event-1"))

        val result = fixture.dispatcher.dispatch(request(limit = 10))

        val pending = fixture.queue.pending(limit = 10).single()
        assertEquals(OutboxDispatchResult(1, 0, 1, 0), result)
        assertEquals("event-1", pending.event.id)
        assertEquals(1, pending.attemptCount)
    }

    @Test
    fun permanentRelayFailureMarksEventFailedAndLeavesItPendingForManualHandling() {
        val fixture = fixture(relayResults = listOf(RelaySendResult.PermanentFailure("bad target")))
        fixture.queue.enqueue(securityEvent("event-1"))

        val result = fixture.dispatcher.dispatch(request(limit = 10))

        val pending = fixture.queue.pending(limit = 10).single()
        assertEquals(OutboxDispatchResult(1, 0, 0, 1), result)
        assertEquals("event-1", pending.event.id)
        assertEquals(1, pending.attemptCount)
    }

    @Test
    fun encryptionFailureDoesNotSendRelayMessageAndMarksEventFailed() {
        val fixture = fixture(encryptionFailures = setOf("event-1"))
        fixture.queue.enqueue(securityEvent("event-1"))

        val result = fixture.dispatcher.dispatch(request(limit = 10))

        val pending = fixture.queue.pending(limit = 10).single()
        assertEquals(OutboxDispatchResult(1, 0, 1, 0), result)
        assertTrue(fixture.relay.sentMessages.isEmpty())
        assertEquals(1, pending.attemptCount)
    }

    @Test
    fun dispatcherContinuesAfterOneFailedEvent() {
        val fixture = fixture(encryptionFailures = setOf("event-1"))
        fixture.queue.enqueue(securityEvent("event-1"))
        fixture.queue.enqueue(securityEvent("event-2"))

        val result = fixture.dispatcher.dispatch(request(limit = 10))

        assertEquals(OutboxDispatchResult(2, 1, 1, 0), result)
        assertEquals(listOf("event-2"), fixture.encryptor.encryptedEventIds)
        assertEquals(listOf("relay-1"), fixture.relay.sentMessages.map { it.messageId })
        assertEquals(listOf("event-1"), fixture.queue.pending(limit = 10).map { it.event.id })
    }

    @Test
    fun nonPositiveLimitAttemptsNoSendsAndReturnsZeroCounts() {
        val fixture = fixture()
        fixture.queue.enqueue(securityEvent("event-1"))

        val zeroResult = fixture.dispatcher.dispatch(request(limit = 0))
        val negativeResult = fixture.dispatcher.dispatch(request(limit = -1))

        assertEquals(OutboxDispatchResult.Empty, zeroResult)
        assertEquals(OutboxDispatchResult.Empty, negativeResult)
        assertTrue(fixture.relay.sentMessages.isEmpty())
        assertEquals(listOf("event-1"), fixture.queue.pending(limit = 10).map { it.event.id })
    }

    @Test
    fun outboxSourceDoesNotImportForbiddenPackages() {
        val sourceRoot = repositoryRoot().resolve("data/outbox/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "features").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
    }

    private fun fixture(
        relayResults: List<RelaySendResult> = emptyList(),
        encryptionFailures: Set<String> = emptySet(),
    ): Fixture {
        val queue = InMemorySecurityEventQueue(StaticEventClock(1_000L))
        val encryptor = RecordingSecurityEventPayloadEncryptor(encryptionFailures)
        val relay = RecordingRelayClient(relayResults)
        val dispatcher = SecurityEventOutboxDispatcher(
            securityEventQueue = queue,
            payloadEncryptor = encryptor,
            relayClient = relay,
            relayMessageIdProvider = SequentialRelayMessageIdProvider(),
            outboxClock = StaticOutboxClock(5_000L),
        )
        return Fixture(queue, encryptor, relay, dispatcher)
    }

    private fun request(limit: Int): OutboxDispatchRequest {
        return OutboxDispatchRequest(
            sourceDeviceId = DeviceId("child-device"),
            targetDeviceId = DeviceId("parent-device"),
            limit = limit,
        )
    }

    private fun securityEvent(id: String): SecurityEvent {
        return SecurityEvent(
            id = id,
            deviceId = DeviceId("child-device"),
            type = SecurityEventType.VPN_STOPPED,
            severity = EventSeverity.CRITICAL,
            createdAtMillis = 123L,
            summary = "VPN protection was stopped.",
        )
    }

    private data class Fixture(
        val queue: SecurityEventQueue,
        val encryptor: RecordingSecurityEventPayloadEncryptor,
        val relay: RecordingRelayClient,
        val dispatcher: SecurityEventOutboxDispatcher,
    )

    private class RecordingSecurityEventPayloadEncryptor(
        private val failingEventIds: Set<String>,
    ) : SecurityEventPayloadEncryptor {
        val encryptedEventIds = mutableListOf<String>()

        override fun encrypt(event: SecurityEvent, targetDeviceId: DeviceId): EncryptedPayload {
            if (event.id in failingEventIds) {
                throw IllegalStateException("encryption failed")
            }
            encryptedEventIds += event.id
            return EncryptedPayload(
                algorithm = "test-encryption",
                ciphertext = byteArrayOf(1, 2, 3),
                nonce = targetDeviceId.value.encodeToByteArray(),
            )
        }
    }

    private class RecordingRelayClient(
        private val configuredResults: List<RelaySendResult>,
    ) : RelayClient {
        val sentMessages = mutableListOf<RelayMessage>()
        private var sendCount = 0

        override fun send(message: RelayMessage): RelaySendResult {
            val result = configuredResults.getOrElse(sendCount) { RelaySendResult.Success }
            sendCount += 1
            if (result == RelaySendResult.Success) {
                sentMessages += message
            }
            return result
        }
    }

    private class SequentialRelayMessageIdProvider : RelayMessageIdProvider {
        private var next = 1

        override fun nextId(): String {
            return "relay-${next++}"
        }
    }

    private class StaticOutboxClock(private val nowMillis: Long) : OutboxClock {
        override fun nowMillis(): Long = nowMillis
    }

    private class StaticEventClock(private val nowMillis: Long) : EventClock {
        override fun nowMillis(): Long = nowMillis
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
