package com.vordain.guard.data.heartbeat

class HeartbeatLeaseEvaluator {
    fun evaluate(
        latestHeartbeat: ProtectionHeartbeat?,
        currentTimeMillis: Long,
        heartbeatTimeoutMillis: Long,
    ): HeartbeatLeaseEvaluation {
        require(heartbeatTimeoutMillis >= 0L) { "heartbeatTimeoutMillis must be non-negative" }

        if (latestHeartbeat == null) {
            return HeartbeatLeaseEvaluation(
                deviceId = null,
                state = HeartbeatLeaseState.MISSING,
                shouldAlertParent = true,
                summary = "Protection is no longer confirmed because no heartbeat is available.",
            )
        }

        val heartbeatAgeMillis = currentTimeMillis - latestHeartbeat.reportedAtMillis
        if (heartbeatAgeMillis > heartbeatTimeoutMillis) {
            return HeartbeatLeaseEvaluation(
                deviceId = latestHeartbeat.deviceId,
                state = HeartbeatLeaseState.STALE,
                shouldAlertParent = true,
                summary = "Protection is no longer confirmed because the child device heartbeat is stale.",
            )
        }

        return HeartbeatLeaseEvaluation(
            deviceId = latestHeartbeat.deviceId,
            state = HeartbeatLeaseState.FRESH,
            shouldAlertParent = false,
            summary = "Protection heartbeat is fresh.",
        )
    }
}
