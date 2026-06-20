package com.vordain.guard.vpn.session

import kotlin.test.Test
import kotlin.test.assertEquals

class VpnRuntimeSnapshotTest {
    private val evaluator = VpnRuntimeSnapshotEvaluator()

    @Test
    fun missingRuntimeEvaluatesToUnknown() {
        val evaluated = evaluator.evaluateFreshness(
            snapshot = VpnRuntimeSnapshot(),
            currentTimeMillis = 10_000L,
        )

        assertEquals(VpnRuntimeSessionState.UNKNOWN, evaluated.sessionState)
    }

    @Test
    fun runningRuntimeBecomesStaleAfterTimeout() {
        val evaluated = evaluator.evaluateFreshness(
            snapshot = VpnRuntimeSnapshot(
                operatingMode = VordainOperatingMode.BASIC_DNS_GUARD,
                sessionState = VpnRuntimeSessionState.RUNNING,
                updatedAtMillis = 1_000L,
                descriptorEstablished = true,
                dnsLoopRunning = true,
            ),
            currentTimeMillis = 70_001L,
        )

        assertEquals(VpnRuntimeSessionState.STALE, evaluated.sessionState)
        assertEquals("STALE_RUNTIME", evaluated.lastErrorCode)
    }

    @Test
    fun stoppedRuntimeDoesNotBecomeStaleRunning() {
        val evaluated = evaluator.evaluateFreshness(
            snapshot = VpnRuntimeSnapshot(
                operatingMode = VordainOperatingMode.BASIC_DNS_GUARD,
                sessionState = VpnRuntimeSessionState.STOPPED,
                updatedAtMillis = 1_000L,
                stoppedAtMillis = 1_000L,
            ),
            currentTimeMillis = 70_001L,
        )

        assertEquals(VpnRuntimeSessionState.STOPPED, evaluated.sessionState)
    }
}
