package com.vordain.guard.features.alerts

import com.vordain.guard.core.events.EventSeverity
import com.vordain.guard.core.events.SecurityEventType

data class AlertListItem(
    val eventId: String,
    val type: SecurityEventType,
    val severity: EventSeverity,
    val summary: String,
    val createdAtMillis: Long,
)
