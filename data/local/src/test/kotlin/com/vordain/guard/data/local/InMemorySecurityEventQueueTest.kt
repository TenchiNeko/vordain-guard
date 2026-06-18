package com.vordain.guard.data.local

import com.vordain.guard.core.events.EventClock
import com.vordain.guard.core.events.EventSeverity
import com.vordain.guard.core.events.SecurityEvent
import com.vordain.guard.core.events.SecurityEventType
import com.vordain.guard.core.model.DeviceId
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemorySecurityEventQueueTest {
    @Test
    fun enqueueCreatesPendingQueuedEvent() {
        val queue = queue(nowMillis = 100L)
        val queued = queue.enqueue(event("event-1"))

        assertEquals("event-1", queued.event.id)
        assertEquals(QueuedEventState.PENDING, queued.state)
        assertEquals(0, queued.attemptCount)
        assertEquals(100L, queued.enqueuedAtMillis)
        assertEquals(null, queued.lastAttemptAtMillis)
    }

    @Test
    fun pendingReturnsEventsInInsertionOrder() {
        val queue = queue()
        queue.enqueue(event("event-1"))
        queue.enqueue(event("event-2"))
        queue.enqueue(event("event-3"))

        assertEquals(listOf("event-1", "event-2", "event-3"), queue.pending(limit = 10).map { it.event.id })
    }

    @Test
    fun pendingRespectsLimit() {
        val queue = queue()
        queue.enqueue(event("event-1"))
        queue.enqueue(event("event-2"))
        queue.enqueue(event("event-3"))

        assertEquals(listOf("event-1", "event-2"), queue.pending(limit = 2).map { it.event.id })
    }

    @Test
    fun markDeliveredHidesEventFromPending() {
        val queue = queue()
        queue.enqueue(event("event-1"))
        queue.enqueue(event("event-2"))

        queue.markDelivered("event-1")

        assertEquals(listOf("event-2"), queue.pending(limit = 10).map { it.event.id })
    }

    @Test
    fun markFailedIncrementsAttemptCount() {
        val clock = MutableEventClock(100L)
        val queue = InMemorySecurityEventQueue(clock)
        queue.enqueue(event("event-1"))

        clock.nowMillis = 250L
        queue.markFailed("event-1")

        val queued = queue.pending(limit = 10).single()
        assertEquals(QueuedEventState.FAILED, queued.state)
        assertEquals(1, queued.attemptCount)
        assertEquals(250L, queued.lastAttemptAtMillis)
    }

    @Test
    fun failedEventRemainsPendingForRetry() {
        val queue = queue()
        queue.enqueue(event("event-1"))
        queue.markFailed("event-1")

        assertEquals(listOf("event-1"), queue.pending(limit = 10).map { it.event.id })
    }

    @Test
    fun removeDeletesEvent() {
        val queue = queue()
        queue.enqueue(event("event-1"))

        queue.remove("event-1")

        assertTrue(queue.pending(limit = 10).isEmpty())
    }

    @Test
    fun unknownOperationsAreSafe() {
        val queue = queue()

        queue.markDelivered("missing")
        queue.markFailed("missing")
        queue.remove("missing")

        assertTrue(queue.pending(limit = 10).isEmpty())
    }

    @Test
    fun duplicateEventIdsDoNotCreateDuplicatePendingEvents() {
        val queue = queue()
        queue.enqueue(event("event-1", summary = "Original"))
        queue.enqueue(event("event-1", summary = "Duplicate"))

        val pending = queue.pending(limit = 10)
        assertEquals(1, pending.size)
        assertEquals("Original", pending.single().event.summary)
    }

    @Test
    fun queueDoesNotExposeMutableEventPayloadReference() {
        val payload = byteArrayOf(1, 2, 3)
        val queue = queue()
        queue.enqueue(event("event-1", encryptedPayload = payload))

        payload[0] = 9
        val queuedPayload = queue.pending(limit = 10).single().event.encryptedPayload

        assertContentEquals(byteArrayOf(1, 2, 3), queuedPayload)
    }

    @Test
    fun dataLocalSourceDoesNotImportForbiddenPackages() {
        val sourceRoot = repositoryRoot().resolve("data/local/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "relay").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "features").joinToString("."))
    }

    private fun queue(nowMillis: Long = 100L): InMemorySecurityEventQueue {
        return InMemorySecurityEventQueue(MutableEventClock(nowMillis))
    }

    private fun event(
        id: String,
        summary: String = "VPN protection was stopped.",
        encryptedPayload: ByteArray? = null,
    ): SecurityEvent {
        return SecurityEvent(
            id = id,
            deviceId = DeviceId("child-device"),
            type = SecurityEventType.VPN_STOPPED,
            severity = EventSeverity.CRITICAL,
            createdAtMillis = 50L,
            summary = summary,
            encryptedPayload = encryptedPayload,
        )
    }

    private class MutableEventClock(var nowMillis: Long) : EventClock {
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
