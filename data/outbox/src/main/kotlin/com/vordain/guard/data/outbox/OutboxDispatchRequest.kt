package com.vordain.guard.data.outbox

import com.vordain.guard.core.model.DeviceId

data class OutboxDispatchRequest(
    val sourceDeviceId: DeviceId,
    val targetDeviceId: DeviceId,
    val limit: Int,
)
