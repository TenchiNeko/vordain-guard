package com.vordain.guard.core.crypto

data class DeviceKeyPair(
    val publicKey: PublicDeviceKey,
    val privateKeyHandle: String,
)
