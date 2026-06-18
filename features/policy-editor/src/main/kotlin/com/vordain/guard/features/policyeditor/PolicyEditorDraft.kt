package com.vordain.guard.features.policyeditor

import com.vordain.guard.core.model.LockdownMode

data class PolicyEditorDraft(
    val mode: LockdownMode,
    val blockUnknownDomains: Boolean,
    val blockKnownProxyDomains: Boolean,
)
