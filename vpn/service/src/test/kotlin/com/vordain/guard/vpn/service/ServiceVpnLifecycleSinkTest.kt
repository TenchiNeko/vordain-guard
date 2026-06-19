package com.vordain.guard.vpn.service

import com.vordain.guard.core.events.EventClock
import com.vordain.guard.core.events.EventIdProvider
import com.vordain.guard.core.events.SecurityEventFactory
import com.vordain.guard.core.events.SecurityEventType
import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.data.local.InMemorySecurityEventQueue
import com.vordain.guard.vpn.lifecycle.DefaultVpnLifecycleIncidentHandler
import com.vordain.guard.vpn.lifecycle.VpnLifecycleIncident
import com.vordain.guard.vpn.lifecycle.VpnLifecycleIncidentHandler
import com.vordain.guard.vpn.lifecycle.VpnLifecycleIncidentResult
import com.vordain.guard.vpn.lifecycle.VpnLifecycleIncidentType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceVpnLifecycleSinkTest {
    @Test
    fun revokedCallbackCreatesVpnRevokedIncident() {
        val handler = RecordingIncidentHandler()
        val sink = sink(handler = handler)

        sink.onVpnRevoked()

        assertEquals(VpnLifecycleIncidentType.VPN_REVOKED, handler.incidents.single().type)
    }

    @Test
    fun revokedCallbackPassesIncidentToHandler() {
        val handler = RecordingIncidentHandler()
        val sink = sink(handler = handler)

        sink.onVpnRevoked()

        assertEquals(1, handler.incidents.size)
        assertEquals("result-1", sink.lastResult?.eventId)
    }

    @Test
    fun revokedCallbackQueuesVpnStoppedEventThroughLifecycleHandler() {
        val queue = InMemorySecurityEventQueue(FixedEventClock(2_000L))
        val sink = sink(
            handler = DefaultVpnLifecycleIncidentHandler(
                securityEventFactory = SecurityEventFactory(
                    eventIdProvider = SequentialEventIdProvider(),
                    eventClock = FixedEventClock(999L),
                ),
                securityEventQueue = queue,
            ),
        )

        sink.onVpnRevoked()
        val queuedEvent = queue.pending(limit = 1).single().event

        assertEquals(SecurityEventType.VPN_STOPPED, queuedEvent.type)
        assertEquals("event-1", sink.lastResult?.eventId)
    }

    @Test
    fun startedCallbackCreatesVpnStartedIncident() {
        val handler = RecordingIncidentHandler()
        val sink = sink(handler = handler)

        sink.onVpnStarted()

        assertEquals(VpnLifecycleIncidentType.VPN_STARTED, handler.incidents.single().type)
    }

    @Test
    fun stoppedCallbackCreatesVpnStoppedIncident() {
        val handler = RecordingIncidentHandler()
        val sink = sink(handler = handler)

        sink.onVpnStopped()

        assertEquals(VpnLifecycleIncidentType.VPN_STOPPED, handler.incidents.single().type)
    }

    @Test
    fun timestampProviderIsUsed() {
        val handler = RecordingIncidentHandler()
        val sink = sink(handler = handler, nowMillis = 42_000L)

        sink.onVpnRevoked()

        assertEquals(42_000L, handler.incidents.single().occurredAtMillis)
    }

    @Test
    fun deviceIdProviderIsUsed() {
        val handler = RecordingIncidentHandler()
        val sink = sink(handler = handler, deviceId = DeviceId("child-2"))

        sink.onVpnRevoked()

        assertEquals(DeviceId("child-2"), handler.incidents.single().deviceId)
    }

    @Test
    fun noOpSinkDoesNotThrow() {
        VpnLifecycleSink.NoOp.onVpnStarted()
        VpnLifecycleSink.NoOp.onVpnStopped()
        VpnLifecycleSink.NoOp.onVpnRevoked()
    }

    @Test
    fun vordainVpnServiceSourceCallsSinkInOnRevoke() {
        val source = repositoryRoot()
            .resolve("vpn/service/src/main/kotlin/com/vordain/guard/vpn/service/VordainVpnService.kt")
            .readText()

        assertTrue(source.contains("override fun onRevoke()"))
        assertTrue(source.contains("lifecycleSink.onVpnRevoked()"))
    }

    @Test
    fun vordainVpnServiceSourceStaysThin() {
        val source = repositoryRoot()
            .resolve("vpn/service/src/main/kotlin/com/vordain/guard/vpn/service/VordainVpnService.kt")
            .readText()

        assertDoesNotContain(source, "PolicyEngine")
        assertDoesNotContain(source, "DomainTrafficEvaluator")
        assertDoesNotContain(source, "DnsMessageParser")
        assertDoesNotContain(source, "RelayClient")
        assertDoesNotContain(source, "backend")
        assertDoesNotContain(source, "outbox")
        assertDoesNotContain(source, ".read(")
        assertDoesNotContain(source, "write(")
        assertDoesNotContain(source, "FileDescriptor")
        assertDoesNotContain(source, "DatagramSocket")
        assertDoesNotContain(source, "Socket(")
    }

    private fun sink(
        handler: VpnLifecycleIncidentHandler,
        deviceId: DeviceId = DeviceId("child-1"),
        nowMillis: Long = 1_000L,
    ): ServiceVpnLifecycleSink {
        return ServiceVpnLifecycleSink(
            deviceIdProvider = FixedDeviceIdProvider(deviceId),
            clock = FixedVpnLifecycleClock(nowMillis),
            incidentHandler = handler,
        )
    }

    private class RecordingIncidentHandler : VpnLifecycleIncidentHandler {
        val incidents = mutableListOf<VpnLifecycleIncident>()

        override fun handle(incident: VpnLifecycleIncident): VpnLifecycleIncidentResult {
            incidents += incident
            return VpnLifecycleIncidentResult(
                eventCreated = true,
                queued = true,
                eventId = "result-${incidents.size}",
            )
        }
    }

    private class FixedDeviceIdProvider(private val deviceId: DeviceId) : VpnLifecycleDeviceIdProvider {
        override fun currentDeviceId(): DeviceId = deviceId
    }

    private class FixedVpnLifecycleClock(private val nowMillis: Long) : VpnLifecycleClock {
        override fun nowMillis(): Long = nowMillis
    }

    private class FixedEventClock(private val nowMillis: Long) : EventClock {
        override fun nowMillis(): Long = nowMillis
    }

    private class SequentialEventIdProvider : EventIdProvider {
        private var nextValue = 1

        override fun nextId(): String {
            return "event-${nextValue++}"
        }
    }

    private fun assertDoesNotContain(source: String, forbiddenText: String) {
        assertTrue(
            actual = !source.contains(forbiddenText),
            message = "Forbidden text $forbiddenText found in VordainVpnService",
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
}
