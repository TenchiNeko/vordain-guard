package com.vordain.guard.data.local

import com.vordain.guard.core.events.SecurityEvent

data class QueuedSecurityEvent(
    val event: SecurityEvent,
    val state: QueuedEventState,
    val attemptCount: Int,
    val enqueuedAtMillis: Long,
    val lastAttemptAtMillis: Long?,
)
