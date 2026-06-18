package com.vordain.guard.features.parentdashboard

import com.vordain.guard.core.model.ProtectionState

data class ParentDashboardSummary(
    val childProtectionState: ProtectionState,
    val unreadAlertCount: Int,
)
