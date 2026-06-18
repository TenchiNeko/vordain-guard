package com.vordain.guard.core.crypto

data class EncryptedPayload(
    val algorithm: String,
    val ciphertext: ByteArray,
    val nonce: ByteArray,
)
