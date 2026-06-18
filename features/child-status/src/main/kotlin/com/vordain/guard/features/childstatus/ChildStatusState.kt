package com.vordain.guard.features.childstatus

import com.vordain.guard.core.model.ProtectionState

data class ChildStatusState(
    val protectionState: ProtectionState,
    val vpnEnabled: Boolean,
)
