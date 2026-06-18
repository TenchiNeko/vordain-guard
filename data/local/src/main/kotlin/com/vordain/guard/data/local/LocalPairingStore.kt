package com.vordain.guard.data.local

import com.vordain.guard.core.model.DeviceId

interface LocalPairingStore {
    fun readPairedParentDeviceId(): DeviceId?

    fun writePairedParentDeviceId(deviceId: DeviceId)

    fun clearPairing()
}
