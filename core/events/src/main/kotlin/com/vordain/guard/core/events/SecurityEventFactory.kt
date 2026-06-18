package com.vordain.guard.core.events

import com.vordain.guard.core.model.DeviceId

class SecurityEventFactory(
    private val eventIdProvider: EventIdProvider,
    private val eventClock: EventClock,
) {
    fun create(input: SecurityEventInput): SecurityEvent {
        return SecurityEvent(
            id = eventIdProvider.nextId(),
            deviceId = input.deviceId,
            type = input.type,
            severity = input.severity,
            createdAtMillis = input.createdAtMillis,
            summary = input.summary,
            encryptedPayload = null,
        )
    }

    fun create(
        deviceId: DeviceId,
        type: SecurityEventType,
        severity: EventSeverity,
        summary: String,
    ): SecurityEvent {
        return create(
            SecurityEventInput(
                deviceId = deviceId,
                type = type,
                severity = severity,
                summary = summary,
                createdAtMillis = eventClock.nowMillis(),
            ),
        )
    }
}
