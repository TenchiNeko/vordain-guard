package com.vordain.guard.core.crypto

interface KeyStoreProvider {
    fun getOrCreateDeviceKeyPair(): DeviceKeyPair

    fun getTrustedParentPublicKey(): PublicDeviceKey?
}
