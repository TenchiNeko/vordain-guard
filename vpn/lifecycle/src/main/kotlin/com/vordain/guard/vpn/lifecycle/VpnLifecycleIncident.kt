package com.vordain.guard.vpn.lifecycle

import com.vordain.guard.core.model.DeviceId

data class VpnLifecycleIncident(
    val deviceId: DeviceId,
    val type: VpnLifecycleIncidentType,
    val occurredAtMillis: Long,
    val summary: String?,
)
