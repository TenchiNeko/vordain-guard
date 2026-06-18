package com.vordain.guard.vpn.lifecycle

import com.vordain.guard.core.events.EventClock
import com.vordain.guard.core.events.EventIdProvider
import com.vordain.guard.core.events.EventSeverity
import com.vordain.guard.core.events.SecurityEventFactory
import com.vordain.guard.core.events.SecurityEventType
import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.data.local.InMemorySecurityEventQueue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefaultVpnLifecycleIncidentHandlerTest {
    @Test
    fun vpnRevokedCreatesVpnStoppedEvent() {
        val fixture = fixture()

        fixture.handler.handle(incident(VpnLifecycleIncidentType.VPN_REVOKED))
        val event = fixture.queue.pending(limit = 1).single().event

        assertEquals(SecurityEventType.VPN_STOPPED, event.type)
        assertEquals(EventSeverity.CRITICAL, event.severity)
    }

    @Test
    fun vpnRevokedQueuesEvent() {
        val fixture = fixture()

        val result = fixture.handler.handle(incident(VpnLifecycleIncidentType.VPN_REVOKED))

        assertTrue(result.queued)
        assertEquals(1, fixture.queue.pending(limit = 10).size)
    }

    @Test
    fun vpnStoppedCreatesVpnStoppedEvent() {
        val fixture = fixture()

        fixture.handler.handle(incident(VpnLifecycleIncidentType.VPN_STOPPED))
        val event = fixture.queue.pending(limit = 1).single().event

        assertEquals(SecurityEventType.VPN_STOPPED, event.type)
        assertEquals(EventSeverity.CRITICAL, event.severity)
    }

    @Test
    fun vpnStartedCreatesVpnRestartedInfoEvent() {
        val fixture = fixture()

        fixture.handler.handle(incident(VpnLifecycleIncidentType.VPN_STARTED))
        val event = fixture.queue.pending(limit = 1).single().event

        assertEquals(SecurityEventType.VPN_RESTARTED, event.type)
        assertEquals(EventSeverity.INFO, event.severity)
    }

    @Test
    fun eventTimestampUsesIncidentOccurredAtMillis() {
        val fixture = fixture()

        fixture.handler.handle(incident(VpnLifecycleIncidentType.VPN_STOPPED, occurredAtMillis = 42_000L))
        val event = fixture.queue.pending(limit = 1).single().event

        assertEquals(42_000L, event.createdAtMillis)
    }

    @Test
    fun eventDeviceIdMatchesIncidentDeviceId() {
        val fixture = fixture()

        fixture.handler.handle(incident(VpnLifecycleIncidentType.VPN_STOPPED, deviceId = DeviceId("child-2")))
        val event = fixture.queue.pending(limit = 1).single().event

        assertEquals(DeviceId("child-2"), event.deviceId)
    }

    @Test
    fun handlerResultIncludesEventId() {
        val fixture = fixture()

        val result = fixture.handler.handle(incident(VpnLifecycleIncidentType.VPN_STOPPED))

        assertTrue(result.eventCreated)
        assertEquals("event-1", result.eventId)
    }

    @Test
    fun queuePendingContainsCreatedEvent() {
        val fixture = fixture()

        val result = fixture.handler.handle(incident(VpnLifecycleIncidentType.VPN_STOPPED))
        val queued = fixture.queue.pending(limit = 1).single()

        assertNotNull(result.eventId)
        assertEquals(result.eventId, queued.event.id)
    }

    @Test
    fun sourceDoesNotImportForbiddenDependencies() {
        val sourceRoot = repositoryRoot().resolve("vpn/lifecycle/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn", "service").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn", "dns").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn", "classifier").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "features").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data", "relay").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data", "outbox").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, "Manager")
        assertSourceTreeDoesNotContain(sourceRoot, "TO" + "DO")
        assertSourceTreeDoesNotContain(sourceRoot, "FIX" + "ME")
    }

    private fun fixture(): Fixture {
        val queueClock = FixedEventClock(nowMillis = 2_000L)
        val queue = InMemorySecurityEventQueue(queueClock)
        val handler = DefaultVpnLifecycleIncidentHandler(
            securityEventFactory = SecurityEventFactory(
                eventIdProvider = SequentialEventIdProvider(),
                eventClock = FixedEventClock(nowMillis = 999L),
            ),
            securityEventQueue = queue,
        )
        return Fixture(handler = handler, queue = queue)
    }

    private fun incident(
        type: VpnLifecycleIncidentType,
        deviceId: DeviceId = DeviceId("child-1"),
        occurredAtMillis: Long = 1_000L,
    ): VpnLifecycleIncident {
        return VpnLifecycleIncident(
            deviceId = deviceId,
            type = type,
            occurredAtMillis = occurredAtMillis,
            summary = null,
        )
    }

    private class SequentialEventIdProvider : EventIdProvider {
        private var nextValue = 1

        override fun nextId(): String {
            return "event-${nextValue++}"
        }
    }

    private class FixedEventClock(private val nowMillis: Long) : EventClock {
        override fun nowMillis(): Long = nowMillis
    }

    private data class Fixture(
        val handler: DefaultVpnLifecycleIncidentHandler,
        val queue: InMemorySecurityEventQueue,
    )

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
