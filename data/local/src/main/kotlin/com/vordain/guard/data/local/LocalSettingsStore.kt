package com.vordain.guard.data.local

import com.vordain.guard.core.model.ProtectionState

interface LocalSettingsStore {
    fun readProtectionState(): ProtectionState

    fun writeProtectionState(state: ProtectionState)
}
