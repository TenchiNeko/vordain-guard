package com.vordain.guard.core.events

import com.vordain.guard.core.model.DeviceId

data class SecurityEventInput(
    val deviceId: DeviceId,
    val type: SecurityEventType,
    val severity: EventSeverity,
    val summary: String,
    val createdAtMillis: Long,
)
