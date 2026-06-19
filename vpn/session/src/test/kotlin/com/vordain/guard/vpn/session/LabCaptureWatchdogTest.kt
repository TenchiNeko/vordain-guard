package com.vordain.guard.vpn.session

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LabCaptureWatchdogTest {
    private val watchdog = LabCaptureWatchdog()

    @Test
    fun watchdogCanStayInactiveForNormalShell() {
        val state = LabCaptureWatchdogState.inactive("normal shell")

        assertFalse(state.active)
        assertFalse(watchdog.isExpired(state, currentTimeMillis = 10_000L))
    }

    @Test
    fun watchdogIsActiveForLabMode() {
        val state = watchdog.start(
            config = LabCaptureWatchdogConfig(maxSessionMillis = 1_000L),
            currentTimeMillis = 5_000L,
            reason = "lab capture active",
        )

        assertTrue(state.active)
        assertFalse(watchdog.isExpired(state, currentTimeMillis = 5_999L))
    }

    @Test
    fun timeoutTriggersExpiredState() {
        val state = watchdog.start(
            config = LabCaptureWatchdogConfig(maxSessionMillis = 1_000L),
            currentTimeMillis = 5_000L,
            reason = "lab capture active",
        )

        assertTrue(watchdog.isExpired(state, currentTimeMillis = 6_000L))
    }

    @Test
    fun stopClearsWatchdogState() {
        val state = watchdog.stop("manual stop")

        assertFalse(state.active)
        assertFalse(watchdog.isExpired(state, currentTimeMillis = Long.MAX_VALUE))
    }
}
