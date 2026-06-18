package com.vordain.guard.core.status

import com.vordain.guard.core.events.EventClock
import com.vordain.guard.core.events.EventIdProvider
import com.vordain.guard.core.events.EventSeverity
import com.vordain.guard.core.events.SecurityEventFactory
import com.vordain.guard.core.events.SecurityEventType
import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.ProtectionState
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProtectionEventFactoryTest {
    @Test
    fun protectedEvaluationCreatesNoEvent() {
        val event = factory().createEvent(
            evaluation(
                state = ProtectionState.PROTECTED,
                reasons = setOf(
                    ProtectionStatusReason.HEARTBEAT_FRESH,
                    ProtectionStatusReason.VPN_ACTIVE,
                    ProtectionStatusReason.POLICY_LOADED,
                ),
                shouldAlertParent = false,
            ),
        )

        assertNull(event)
    }

    @Test
    fun unknownEvaluationCreatesProtectionUnknownEvent() {
        val event = factory().createEvent(
            evaluation(
                state = ProtectionState.UNKNOWN,
                reasons = setOf(ProtectionStatusReason.HEARTBEAT_STALE),
            ),
        )

        assertEquals(SecurityEventType.CHILD_DEVICE_OFFLINE, event?.type)
        assertEquals("Protection is no longer confirmed because the child device stopped reporting.", event?.summary)
    }

    @Test
    fun unknownEventSeverityIsHigh() {
        val event = factory().createEvent(
            evaluation(
                state = ProtectionState.UNKNOWN,
                reasons = setOf(ProtectionStatusReason.HEARTBEAT_MISSING),
            ),
        )

        assertEquals(EventSeverity.HIGH, event?.severity)
    }

    @Test
    fun stoppedDueToVpnStoppedCreatesVpnStoppedEvent() {
        val event = factory().createEvent(
            evaluation(
                state = ProtectionState.STOPPED,
                reasons = setOf(
                    ProtectionStatusReason.HEARTBEAT_FRESH,
                    ProtectionStatusReason.VPN_STOPPED,
                ),
            ),
        )

        assertEquals(SecurityEventType.VPN_STOPPED, event?.type)
        assertEquals("VPN protection was stopped or disabled.", event?.summary)
    }

    @Test
    fun vpnStoppedEventSeverityIsCritical() {
        val event = factory().createEvent(
            evaluation(
                state = ProtectionState.STOPPED,
                reasons = setOf(ProtectionStatusReason.VPN_STOPPED),
            ),
        )

        assertEquals(EventSeverity.CRITICAL, event?.severity)
    }

    @Test
    fun stoppedDueToLocalTamperCreatesTamperSuspectedEvent() {
        val event = factory().createEvent(
            evaluation(
                state = ProtectionState.STOPPED,
                reasons = setOf(ProtectionStatusReason.LOCAL_TAMPER_DETECTED),
            ),
        )

        assertEquals(SecurityEventType.TAMPER_SUSPECTED, event?.type)
        assertEquals("Local tamper was detected on the child device.", event?.summary)
    }

    @Test
    fun tamperEventSeverityIsCritical() {
        val event = factory().createEvent(
            evaluation(
                state = ProtectionState.STOPPED,
                reasons = setOf(ProtectionStatusReason.LOCAL_TAMPER_DETECTED),
            ),
        )

        assertEquals(EventSeverity.CRITICAL, event?.severity)
    }

    @Test
    fun tamperWinsOverVpnStoppedWhenBothReasonsArePresent() {
        val event = factory().createEvent(
            evaluation(
                state = ProtectionState.STOPPED,
                reasons = setOf(
                    ProtectionStatusReason.VPN_STOPPED,
                    ProtectionStatusReason.LOCAL_TAMPER_DETECTED,
                ),
            ),
        )

        assertEquals(SecurityEventType.TAMPER_SUSPECTED, event?.type)
    }

    @Test
    fun degradedEvaluationCreatesNoEventUntilDedicatedEventTypeIsAvailable() {
        val event = factory().createEvent(
            evaluation(
                state = ProtectionState.DEGRADED,
                reasons = setOf(
                    ProtectionStatusReason.HEARTBEAT_FRESH,
                    ProtectionStatusReason.VPN_ACTIVE,
                    ProtectionStatusReason.ALWAYS_ON_VPN_DISABLED,
                ),
            ),
        )

        assertNull(event)
    }

    @Test
    fun eventIdAndTimeAreDeterministic() {
        val event = factory(id = "event-status-1", nowMillis = 42_000L).createEvent(
            evaluation(
                state = ProtectionState.UNKNOWN,
                reasons = setOf(ProtectionStatusReason.HEARTBEAT_STALE),
            ),
        )

        assertEquals("event-status-1", event?.id)
        assertEquals(42_000L, event?.createdAtMillis)
        assertEquals(deviceId, event?.deviceId)
    }

    @Test
    fun statusSourceDoesNotImportForbiddenPackages() {
        val sourceRoot = repositoryRoot().resolve("core/status/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "features").joinToString("."))
    }

    private fun factory(
        id: String = "event-1",
        nowMillis: Long = 1_234L,
    ): ProtectionEventFactory {
        return ProtectionEventFactory(
            SecurityEventFactory(
                eventIdProvider = StaticEventIdProvider(id),
                eventClock = StaticEventClock(nowMillis),
            ),
        )
    }

    private fun evaluation(
        state: ProtectionState,
        reasons: Set<ProtectionStatusReason>,
        shouldAlertParent: Boolean = true,
    ): ProtectionEvaluation {
        return ProtectionEvaluation(
            deviceId = deviceId,
            state = state,
            reasons = reasons,
            shouldAlertParent = shouldAlertParent,
            summary = "status summary",
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

    private companion object {
        val deviceId = DeviceId("child-device")
    }
}
