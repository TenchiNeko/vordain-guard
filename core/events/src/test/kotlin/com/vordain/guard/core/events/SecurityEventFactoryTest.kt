package com.vordain.guard.core.events

import com.vordain.guard.core.model.DeviceId
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecurityEventFactoryTest {
    @Test
    fun createsDeterministicEventWithInjectedIdAndClock() {
        val event = factory(id = "event-1", nowMillis = 1234L).create(
            deviceId = DeviceId("child-device"),
            type = SecurityEventType.POLICY_UPDATED,
            severity = EventSeverity.INFO,
            summary = "Policy updated",
        )

        assertEquals("event-1", event.id)
        assertEquals(DeviceId("child-device"), event.deviceId)
        assertEquals(SecurityEventType.POLICY_UPDATED, event.type)
        assertEquals(EventSeverity.INFO, event.severity)
        assertEquals(1234L, event.createdAtMillis)
        assertEquals("Policy updated", event.summary)
        assertNull(event.encryptedPayload)
    }

    @Test
    fun createsEventFromExplicitInputTimestamp() {
        val event = factory(id = "event-2", nowMillis = 9999L).create(
            SecurityEventInput(
                deviceId = DeviceId("child-device"),
                type = SecurityEventType.CHILD_DEVICE_OFFLINE,
                severity = EventSeverity.HIGH,
                summary = "Child device appears offline",
                createdAtMillis = 5678L,
            ),
        )

        assertEquals("event-2", event.id)
        assertEquals(5678L, event.createdAtMillis)
    }

    @Test
    fun vpnStoppedEventCanBeCreatedAsCritical() {
        val event = factory(id = "event-vpn-stopped", nowMillis = 1234L).create(
            deviceId = DeviceId("child-device"),
            type = SecurityEventType.VPN_STOPPED,
            severity = EventSeverity.CRITICAL,
            summary = "VPN protection stopped",
        )

        assertEquals(SecurityEventType.VPN_STOPPED, event.type)
        assertEquals(EventSeverity.CRITICAL, event.severity)
    }

    @Test
    fun coreEventsSourceDoesNotImportAndroidVpnDataOrBackendPackages() {
        val sourceRoot = repositoryRoot().resolve("core/events/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
    }

    private fun factory(id: String, nowMillis: Long): SecurityEventFactory {
        return SecurityEventFactory(
            eventIdProvider = StaticEventIdProvider(id),
            eventClock = StaticEventClock(nowMillis),
        )
    }

    private class StaticEventIdProvider(private val id: String) : EventIdProvider {
        override fun nextId(): String = id
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
