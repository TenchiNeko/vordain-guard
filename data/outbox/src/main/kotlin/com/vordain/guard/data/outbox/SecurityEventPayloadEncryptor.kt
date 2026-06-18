package com.vordain.guard.data.outbox

import com.vordain.guard.core.crypto.EncryptedPayload
import com.vordain.guard.core.events.SecurityEvent
import com.vordain.guard.core.model.DeviceId

interface SecurityEventPayloadEncryptor {
    fun encrypt(event: SecurityEvent, targetDeviceId: DeviceId): EncryptedPayload
}
