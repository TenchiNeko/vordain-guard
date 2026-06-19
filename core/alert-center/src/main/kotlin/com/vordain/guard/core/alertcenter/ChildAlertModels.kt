package com.vordain.guard.core.alertcenter

import com.vordain.guard.core.model.DeviceId

enum class AlertType {
    VPN_STOPPED,
    VPN_REVOKED,
    BASIC_DNS_GUARD_STOPPED,
    BASIC_DNS_GUARD_HEARTBEAT_STALE,
    BASIC_DNS_GUARD_HEARTBEAT_MISSING,
    PIN_COMPROMISE_SUSPECTED,
    HARDENING_NEEDS_ATTENTION,
    POLICY_REJECTED,
    POLICY_RESTORED_FAILED,
    DNS_GUARD_STARTED,
    DNS_GUARD_STOPPED,
    DIAGNOSTICS_GENERATED,
}

enum class AlertSeverity {
    INFO,
    WARNING,
    HIGH,
    CRITICAL,
}

enum class AlertStatus {
    ACTIVE,
    ACKNOWLEDGED,
    CLEARED,
}

data class ChildAlert(
    val id: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val status: AlertStatus,
    val childDeviceId: DeviceId,
    val occurredAtMillis: Long,
    val title: String,
    val detail: String,
    val sourceLabel: String,
    val policyVersion: String? = null,
)

data class ChildAlertTimeline(
    val alerts: List<ChildAlert> = emptyList(),
)

class ChildAlertReducer {
    fun append(
        timeline: ChildAlertTimeline,
        alert: ChildAlert,
        maxAlerts: Int,
    ): ChildAlertTimeline {
        require(maxAlerts > 0) { "maxAlerts must be positive" }
        return ChildAlertTimeline(
            alerts = (listOf(alert) + timeline.alerts).take(maxAlerts),
        )
    }

    fun acknowledge(
        timeline: ChildAlertTimeline,
        alertId: String,
        acknowledgedAtMillis: Long,
    ): ChildAlertTimeline {
        return updateStatus(timeline, alertId, AlertStatus.ACKNOWLEDGED, acknowledgedAtMillis)
    }

    fun clear(
        timeline: ChildAlertTimeline,
        alertId: String,
        clearedAtMillis: Long,
    ): ChildAlertTimeline {
        return updateStatus(timeline, alertId, AlertStatus.CLEARED, clearedAtMillis)
    }

    fun activeCriticalCount(timeline: ChildAlertTimeline): Int {
        return timeline.alerts.count { alert ->
            alert.severity == AlertSeverity.CRITICAL && alert.status == AlertStatus.ACTIVE
        }
    }

    fun latest(
        timeline: ChildAlertTimeline,
        count: Int,
    ): List<ChildAlert> {
        require(count >= 0) { "count must not be negative" }
        return timeline.alerts.take(count)
    }

    private fun updateStatus(
        timeline: ChildAlertTimeline,
        alertId: String,
        status: AlertStatus,
        changedAtMillis: Long,
    ): ChildAlertTimeline {
        return timeline.copy(
            alerts = timeline.alerts.map { alert ->
                if (alert.id == alertId) {
                    alert.copy(
                        status = status,
                        detail = "${alert.detail} Status changed at $changedAtMillis.",
                    )
                } else {
                    alert
                }
            },
        )
    }
}
