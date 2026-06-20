package com.vordain.guard.core.auditlog

import com.vordain.guard.core.model.DeviceId

enum class AuditEntryType {
    VPN_PERMISSION_REQUESTED,
    VPN_SHELL_STARTED,
    BASIC_DNS_GUARD_STARTED,
    BASIC_DNS_GUARD_STOPPED,
    BASIC_DNS_GUARD_START_NEEDS_ATTENTION,
    BASIC_DNS_DIAGNOSTICS_COPIED,
    DNS_ONLY_LAB_STARTED,
    DNS_ONLY_LAB_STOPPED,
    FULL_TUNNEL_LAB_STARTED,
    FULL_TUNNEL_LAB_STOPPED,
    POLICY_PAYLOAD_APPLIED,
    POLICY_PAYLOAD_REJECTED,
    POLICY_RESTORED,
    SETUP_REPORT_GENERATED,
    BYPASS_REPORT_GENERATED,
    STATUS_REPORT_GENERATED,
    MAINTENANCE_WINDOW_STARTED,
    MAINTENANCE_WINDOW_ENDED,
    PIN_COMPROMISE_SIGNAL,
    DIAGNOSTICS_COPIED,
    PARENT_POLICY_EDITED,
    PARENT_POLICY_SHARED,
    CHILD_DNS_GUARD_STATUS_IMPORTED,
    SYNC_BUNDLE_CREATED,
    SYNC_BUNDLE_IMPORTED,
    SYNC_BUNDLE_REJECTED,
    PARENT_SYNC_BUNDLE_SHARED,
    CHILD_SYNC_BUNDLE_SHARED,
    POLICY_APPLIED_FROM_BUNDLE,
    POLICY_REJECTED_FROM_BUNDLE,
}

enum class AuditSeverity {
    INFO,
    WARNING,
    HIGH,
}

data class AuditEntry(
    val id: String,
    val type: AuditEntryType,
    val severity: AuditSeverity,
    val occurredAtMillis: Long,
    val title: String,
    val detail: String,
    val deviceId: DeviceId? = null,
)

data class AuditTimeline(
    val entries: List<AuditEntry> = emptyList(),
)

class AuditTimelineReducer {
    fun append(
        timeline: AuditTimeline,
        entry: AuditEntry,
        maxEntries: Int,
    ): AuditTimeline {
        require(maxEntries > 0) { "maxEntries must be positive" }
        return AuditTimeline(
            entries = (listOf(entry) + timeline.entries).take(maxEntries),
        )
    }

    fun clear(timeline: AuditTimeline): AuditTimeline = timeline.copy(entries = emptyList())

    fun latest(
        timeline: AuditTimeline,
        count: Int,
    ): List<AuditEntry> {
        require(count >= 0) { "count must not be negative" }
        return timeline.entries.take(count)
    }
}
