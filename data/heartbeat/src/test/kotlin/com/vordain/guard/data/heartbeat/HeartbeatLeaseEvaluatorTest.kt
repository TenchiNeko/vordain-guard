package com.vordain.guard.data.heartbeat

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.model.ProtectionState
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeartbeatLeaseEvaluatorTest {
    @Test
    fun freshHeartbeatEvaluatesToFreshAndDoesNotAlertParent() {
        val evaluation = evaluator.evaluate(
            latestHeartbeat = heartbeat(reportedAtMillis = 9_000L),
            currentTimeMillis = 10_000L,
            heartbeatTimeoutMillis = 5_000L,
        )

        assertEquals(HeartbeatLeaseState.FRESH, evaluation.state)
        assertFalse(evaluation.shouldAlertParent)
        assertEquals(DeviceId("child-device"), evaluation.deviceId)
    }

    @Test
    fun missingHeartbeatEvaluatesToMissingAndAlertsParent() {
        val evaluation = evaluator.evaluate(
            latestHeartbeat = null,
            currentTimeMillis = 10_000L,
            heartbeatTimeoutMillis = 5_000L,
        )

        assertEquals(HeartbeatLeaseState.MISSING, evaluation.state)
        assertTrue(evaluation.shouldAlertParent)
        assertEquals(null, evaluation.deviceId)
    }

    @Test
    fun staleHeartbeatEvaluatesToStaleAndAlertsParent() {
        val evaluation = evaluator.evaluate(
            latestHeartbeat = heartbeat(reportedAtMillis = 4_999L),
            currentTimeMillis = 10_000L,
            heartbeatTimeoutMillis = 5_000L,
        )

        assertEquals(HeartbeatLeaseState.STALE, evaluation.state)
        assertTrue(evaluation.shouldAlertParent)
        assertEquals(DeviceId("child-device"), evaluation.deviceId)
    }

    @Test
    fun exactTimeoutBoundaryIsFresh() {
        val evaluation = evaluator.evaluate(
            latestHeartbeat = heartbeat(reportedAtMillis = 5_000L),
            currentTimeMillis = 10_000L,
            heartbeatTimeoutMillis = 5_000L,
        )

        assertEquals(HeartbeatLeaseState.FRESH, evaluation.state)
        assertFalse(evaluation.shouldAlertParent)
    }

    @Test
    fun negativeTimeoutIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            evaluator.evaluate(
                latestHeartbeat = heartbeat(),
                currentTimeMillis = 10_000L,
                heartbeatTimeoutMillis = -1L,
            )
        }
    }

    private fun heartbeat(
        reportedAtMillis: Long = 9_000L,
        sequenceNumber: Long = 7L,
    ): ProtectionHeartbeat {
        return ProtectionHeartbeat(
            deviceId = DeviceId("child-device"),
            reportedAtMillis = reportedAtMillis,
            sequenceNumber = sequenceNumber,
            protectionState = ProtectionState.PROTECTED,
            policyId = PolicyId("policy-1"),
        )
    }

    private companion object {
        val evaluator = HeartbeatLeaseEvaluator()
    }
}
