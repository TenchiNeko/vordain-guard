package com.vordain.guard.vpn.engine

import com.vordain.guard.core.model.ProtectionState

interface VpnEngine {
    fun start()

    fun stop()

    fun status(): ProtectionState
}
