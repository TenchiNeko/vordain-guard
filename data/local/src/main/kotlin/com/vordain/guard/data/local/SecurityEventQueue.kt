package com.vordain.guard.data.local

import com.vordain.guard.core.events.SecurityEvent

interface SecurityEventQueue {
    fun enqueue(event: SecurityEvent): QueuedSecurityEvent

    fun pending(limit: Int): List<QueuedSecurityEvent>

    fun markDelivered(eventId: String)

    fun markFailed(eventId: String)

    fun remove(eventId: String)
}
