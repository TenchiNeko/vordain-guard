package com.vordain.guard.data.heartbeat

import com.vordain.guard.core.model.DeviceId

data class HeartbeatLeaseEvaluation(
    val deviceId: DeviceId?,
    val state: HeartbeatLeaseState,
    val shouldAlertParent: Boolean,
    val summary: String,
)
