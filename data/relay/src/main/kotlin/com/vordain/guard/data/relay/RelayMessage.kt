package com.vordain.guard.data.relay

import com.vordain.guard.core.crypto.EncryptedPayload

data class RelayMessage(
    val id: String,
    val destinationDeviceId: String,
    val encryptedPayload: EncryptedPayload,
    val createdAtMillis: Long,
)
