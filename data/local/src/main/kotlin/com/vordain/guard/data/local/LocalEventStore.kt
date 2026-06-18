package com.vordain.guard.data.local

import com.vordain.guard.core.events.SecurityEvent

interface LocalEventStore {
    fun append(event: SecurityEvent)

    fun pendingEvents(limit: Int): List<SecurityEvent>

    fun markRelayed(eventId: String)
}
