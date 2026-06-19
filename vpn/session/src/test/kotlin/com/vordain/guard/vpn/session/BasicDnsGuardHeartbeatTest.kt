package com.vordain.guard.vpn.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BasicDnsGuardHeartbeatTest {
    private val heartbeat = BasicDnsGuardHeartbeat()

    @Test
    fun heartbeatFreshAfterTick() {
        val started = heartbeat.start(VordainOperatingMode.BASIC_DNS_GUARD, currentTimeMillis = 1_000L)
        val ticked = heartbeat.tick(started, currentTimeMillis = 2_000L)
        val evaluated = heartbeat.evaluate(ticked, currentTimeMillis = 3_000L)

        assertEquals(BasicDnsGuardHeartbeatStatus.FRESH, evaluated.status)
        assertEquals("Heartbeat fresh", evaluated.statusLabel)
        assertEquals(2, evaluated.tickCount)
    }

    @Test
    fun heartbeatStaleAfterTimeout() {
        val started = heartbeat.start(
            mode = VordainOperatingMode.BASIC_DNS_GUARD,
            currentTimeMillis = 1_000L,
            staleAfterMillis = 500L,
        )
        val evaluated = heartbeat.evaluate(started, currentTimeMillis = 1_600L)

        assertEquals(BasicDnsGuardHeartbeatStatus.STALE, evaluated.status)
        assertEquals("Heartbeat stale", evaluated.statusLabel)
    }

    @Test
    fun stoppedServiceStopsTicking() {
        val started = heartbeat.start(VordainOperatingMode.BASIC_DNS_GUARD, currentTimeMillis = 1_000L)
        val stopped = heartbeat.stop(started, reason = "Heartbeat stopped")
        val ticked = heartbeat.tick(stopped, currentTimeMillis = 2_000L)

        assertEquals(BasicDnsGuardHeartbeatStatus.STOPPED, ticked.status)
        assertEquals(1, ticked.tickCount)
    }

    @Test
    fun normalShellDoesNotClaimBasicDnsGuardHeartbeat() {
        val started = heartbeat.start(VordainOperatingMode.ESTABLISH_ONLY_SHELL, currentTimeMillis = 1_000L)

        assertFalse(heartbeat.isBasicDnsGuardHeartbeat(started))
    }

    @Test
    fun basicDnsGuardHeartbeatIsRecognized() {
        val started = heartbeat.start(VordainOperatingMode.BASIC_DNS_GUARD, currentTimeMillis = 1_000L)

        assertTrue(heartbeat.isBasicDnsGuardHeartbeat(started))
    }
}
