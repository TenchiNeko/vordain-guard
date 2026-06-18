package com.vordain.guard.core.status

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.ProtectionState

data class ProtectionEvaluation(
    val deviceId: DeviceId,
    val state: ProtectionState,
    val reasons: Set<ProtectionStatusReason>,
    val shouldAlertParent: Boolean,
    val summary: String,
)
