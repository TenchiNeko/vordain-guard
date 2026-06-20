package com.vordain.guard.core.syncbundle

import com.vordain.guard.core.model.DeviceId

enum class SyncBundleDirection {
    CHILD_TO_PARENT,
    PARENT_TO_CHILD,
}

enum class SyncBundleKind {
    CHILD_STATUS_EXPORT,
    PARENT_POLICY_UPDATE,
    PAIRING_HANDOFF,
    DIAGNOSTICS_EXPORT,
}

enum class SyncBundlePayloadKind {
    CHILD_SECURITY_STATUS_REPORT,
    CHILD_ALERT_REPORT,
    BYPASS_RISK_REPORT,
    HARDENING_SETUP_REPORT,
    AUDIT_SUMMARY,
    ACTIVE_POLICY_SUMMARY,
    POLICY_UPDATE,
    PAIRING_INVITE,
    PAIRING_ACCEPTANCE,
    DIAGNOSTICS_TEXT,
}

data class SyncBundlePayload(
    val kind: SyncBundlePayloadKind,
    val label: String,
    val payloadText: String,
)

data class SyncBundle(
    val bundleId: String,
    val direction: SyncBundleDirection,
    val kind: SyncBundleKind,
    val createdAtMillis: Long,
    val sourceDeviceId: DeviceId,
    val targetDeviceId: DeviceId?,
    val payloads: List<SyncBundlePayload>,
    val warningText: String = WARNING_TEXT,
) {
    companion object {
        const val WARNING_TEXT = "Local debug bundle only. Production sync will use encrypted relay later."
    }
}

data class SyncBundleValidationResult(
    val accepted: Boolean,
    val reason: String,
    val bundle: SyncBundle? = null,
)
