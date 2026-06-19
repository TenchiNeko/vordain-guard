package com.vordain.guard.vpn.service

import com.vordain.guard.vpn.session.VpnSessionSnapshot
import com.vordain.guard.vpn.session.VpnSessionState
import com.vordain.guard.vpn.session.VpnSessionStateReason
import com.vordain.guard.vpn.session.VpnSessionStateReducer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ServiceVpnSessionSinkTest {
    @Test
    fun startedCallbackMarksSessionRunning() {
        val sink = sink(startingSnapshot())

        sink.onVpnStarted()

        assertEquals(VpnSessionState.RUNNING, sink.currentSnapshot.state)
        assertEquals(VpnSessionStateReason.STARTED, sink.currentSnapshot.reason)
        assertTrue(sink.lastTransition?.accepted == true)
    }

    @Test
    fun stoppedCallbackMarksRunningSessionStopped() {
        val sink = sink(runningSnapshot())

        sink.onVpnStopped()

        assertEquals(VpnSessionState.STOPPED, sink.currentSnapshot.state)
        assertEquals(VpnSessionStateReason.STOPPED_BY_APP, sink.currentSnapshot.reason)
    }

    @Test
    fun revokedCallbackMarksAnySessionRevoked() {
        val sink = sink(VpnSessionSnapshot.initial(createdAtMillis = 1L))

        sink.onVpnRevoked()

        assertEquals(VpnSessionState.REVOKED, sink.currentSnapshot.state)
        assertEquals(VpnSessionStateReason.REVOKED_BY_SYSTEM, sink.currentSnapshot.reason)
    }

    @Test
    fun timestampProviderIsUsed() {
        val sink = sink(startingSnapshot(), nowMillis = 44_000L)

        sink.onVpnStarted()

        assertEquals(44_000L, sink.currentSnapshot.updatedAtMillis)
    }

    @Test
    fun invalidStoppedCallbackKeepsPreviousSnapshot() {
        val initial = VpnSessionSnapshot.initial(createdAtMillis = 1L)
        val sink = sink(initial)

        sink.onVpnStopped()

        assertFalse(sink.lastTransition?.accepted ?: true)
        assertEquals(initial, sink.currentSnapshot)
    }

    @Test
    fun noOpSessionSinkDoesNotThrow() {
        VpnSessionSink.NoOp.onVpnStarted()
        VpnSessionSink.NoOp.onVpnStopped()
        VpnSessionSink.NoOp.onVpnRevoked()
        VpnSessionSink.NoOp.onVpnError("ignored")
    }

    @Test
    fun errorCallbackMarksSessionError() {
        val sink = sink(runningSnapshot(), nowMillis = 12_000L)

        sink.onVpnError("establish failed")

        assertEquals(VpnSessionState.ERROR, sink.currentSnapshot.state)
        assertEquals(VpnSessionStateReason.PLATFORM_ERROR, sink.currentSnapshot.reason)
        assertEquals("establish failed", sink.currentSnapshot.message)
        assertEquals(12_000L, sink.currentSnapshot.updatedAtMillis)
    }

    @Test
    fun vordainVpnServiceSourceCallsSessionSinkInOnRevoke() {
        val source = serviceSource()

        assertTrue(source.contains("override fun onRevoke()"))
        assertTrue(source.contains("sessionSink.onVpnRevoked()"))
    }

    @Test
    fun vordainVpnServiceSourceStaysThinForSessionWiring() {
        val source = serviceSource()

        assertDoesNotContain(source, "PolicyEngine")
        assertDoesNotContain(source, "DomainTrafficEvaluator")
        assertDoesNotContain(source, "DnsMessageParser")
        assertDoesNotContain(source, "RelayClient")
        assertDoesNotContain(source, "backend")
        assertDoesNotContain(source, "data.outbox")
        assertDoesNotContain(source, "data.relay")
        assertDoesNotContain(source, ".read(")
        assertDoesNotContain(source, "write(")
        assertDoesNotContain(source, "FileDescriptor")
        assertDoesNotContain(source, "DatagramSocket")
        assertDoesNotContain(source, "Socket(")
    }

    @Test
    fun sessionSinkRecordsLastTransition() {
        val sink = sink(startingSnapshot())

        sink.onVpnStarted()

        assertNotNull(sink.lastTransition)
        assertEquals(VpnSessionState.STARTING, sink.lastTransition?.previous?.state)
        assertEquals(VpnSessionState.RUNNING, sink.lastTransition?.next?.state)
    }

    private fun sink(
        initialSnapshot: VpnSessionSnapshot,
        nowMillis: Long = 1_000L,
    ): ServiceVpnSessionSink {
        return ServiceVpnSessionSink(
            stateReducer = VpnSessionStateReducer(),
            initialSnapshot = initialSnapshot,
            clock = FixedVpnSessionClock(nowMillis),
        )
    }

    private fun startingSnapshot(): VpnSessionSnapshot {
        return VpnSessionSnapshot(
            state = VpnSessionState.STARTING,
            reason = VpnSessionStateReason.START_REQUESTED,
            updatedAtMillis = 100L,
            message = null,
        )
    }

    private fun runningSnapshot(): VpnSessionSnapshot {
        return VpnSessionSnapshot(
            state = VpnSessionState.RUNNING,
            reason = VpnSessionStateReason.STARTED,
            updatedAtMillis = 200L,
            message = null,
        )
    }

    private class FixedVpnSessionClock(private val nowMillis: Long) : VpnSessionClock {
        override fun nowMillis(): Long = nowMillis
    }

    private fun serviceSource(): String {
        return repositoryRoot()
            .resolve("vpn/service/src/main/kotlin/com/vordain/guard/vpn/service/VordainVpnService.kt")
            .readText()
    }

    private fun assertDoesNotContain(source: String, forbiddenText: String) {
        assertTrue(
            actual = !source.contains(forbiddenText),
            message = "Forbidden text $forbiddenText found in VordainVpnService",
        )
    }

    private fun repositoryRoot(): File {
        val userDir = System.getProperty("user.dir") ?: "."
        var current = File(userDir).absoluteFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) {
                return current
            }
            val parent = current.parentFile
                ?: error("Could not find repository root from $userDir")
            current = parent
        }
    }
}
