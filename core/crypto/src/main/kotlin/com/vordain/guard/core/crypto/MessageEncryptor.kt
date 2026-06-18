package com.vordain.guard.core.crypto

interface MessageEncryptor {
    fun encryptForParent(plaintext: ByteArray, parentPublicKey: PublicDeviceKey): EncryptedPayload

    fun decryptFromChild(payload: EncryptedPayload, keyPair: DeviceKeyPair): ByteArray
}
