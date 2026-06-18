package com.vordain.guard.data.relay

import com.vordain.guard.core.crypto.EncryptedPayload
import com.vordain.guard.core.model.DeviceId

data class RelayMessage(
    val messageId: String,
    val sourceDeviceId: DeviceId,
    val targetDeviceId: DeviceId,
    val payload: EncryptedPayload,
    val createdAtMillis: Long,
)
