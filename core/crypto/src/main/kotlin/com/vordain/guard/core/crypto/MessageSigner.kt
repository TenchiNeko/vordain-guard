package com.vordain.guard.core.crypto

interface MessageSigner {
    fun signPolicy(policyBytes: ByteArray, keyPair: DeviceKeyPair): ByteArray

    fun verifyPolicySignature(policyBytes: ByteArray, signature: ByteArray, parentPublicKey: PublicDeviceKey): Boolean
}
