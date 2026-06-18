package com.vordain.guard.core.events

import com.vordain.guard.core.model.DeviceId

data class SecurityEvent(
    val id: String,
    val deviceId: DeviceId,
    val type: SecurityEventType,
    val severity: EventSeverity,
    val createdAtMillis: Long,
    val summary: String,
    val encryptedPayload: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SecurityEvent) return false

        return id == other.id &&
            deviceId == other.deviceId &&
            type == other.type &&
            severity == other.severity &&
            createdAtMillis == other.createdAtMillis &&
            summary == other.summary &&
            encryptedPayload.contentEquals(other.encryptedPayload)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + deviceId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + severity.hashCode()
        result = 31 * result + createdAtMillis.hashCode()
        result = 31 * result + summary.hashCode()
        result = 31 * result + (encryptedPayload?.contentHashCode() ?: 0)
        return result
    }
}
