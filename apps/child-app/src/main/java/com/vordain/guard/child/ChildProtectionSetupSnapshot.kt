package com.vordain.guard.child

import com.vordain.guard.core.model.ProtectionState

data class ChildProtectionSetupSnapshot(
    val checklist: VpnSetupChecklist,
    val protectionState: ProtectionState,
) {
    init {
        require(protectionState != ProtectionState.PROTECTED || checklist.isComplete) {
            "Confirmed setup state requires a complete local setup checklist"
        }
    }
}
