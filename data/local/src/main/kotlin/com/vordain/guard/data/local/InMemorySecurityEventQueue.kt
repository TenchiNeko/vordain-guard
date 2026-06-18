package com.vordain.guard.data.local

import com.vordain.guard.core.events.EventClock
import com.vordain.guard.core.events.SecurityEvent

class InMemorySecurityEventQueue(
    private val eventClock: EventClock,
) : SecurityEventQueue {
    private val eventsById = linkedMapOf<String, QueuedSecurityEvent>()

    override fun enqueue(event: SecurityEvent): QueuedSecurityEvent {
        val existing = eventsById[event.id]
        if (existing != null) {
            return existing.copy(event = existing.event.copyForQueue())
        }

        val queued = QueuedSecurityEvent(
            event = event.copyForQueue(),
            state = QueuedEventState.PENDING,
            attemptCount = 0,
            enqueuedAtMillis = eventClock.nowMillis(),
            lastAttemptAtMillis = null,
        )
        eventsById[event.id] = queued
        return queued.copy(event = queued.event.copyForQueue())
    }

    override fun pending(limit: Int): List<QueuedSecurityEvent> {
        require(limit >= 0) { "limit must be non-negative" }
        return eventsById.values
            .asSequence()
            .filter { queued -> queued.state == QueuedEventState.PENDING || queued.state == QueuedEventState.FAILED }
            .take(limit)
            .map { queued -> queued.copy(event = queued.event.copyForQueue()) }
            .toList()
    }

    override fun markDelivered(eventId: String) {
        update(eventId) { queued ->
            queued.copy(state = QueuedEventState.DELIVERED)
        }
    }

    override fun markFailed(eventId: String) {
        update(eventId) { queued ->
            queued.copy(
                state = QueuedEventState.FAILED,
                attemptCount = queued.attemptCount + 1,
                lastAttemptAtMillis = eventClock.nowMillis(),
            )
        }
    }

    override fun remove(eventId: String) {
        eventsById.remove(eventId)
    }

    private fun update(
        eventId: String,
        transform: (QueuedSecurityEvent) -> QueuedSecurityEvent,
    ) {
        val queued = eventsById[eventId] ?: return
        eventsById[eventId] = transform(queued)
    }

    private fun SecurityEvent.copyForQueue(): SecurityEvent {
        return copy(encryptedPayload = encryptedPayload?.copyOf())
    }
}
