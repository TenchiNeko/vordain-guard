package com.vordain.guard.child

import android.content.Context
import com.vordain.guard.core.alertcenter.AlertSeverity
import com.vordain.guard.core.alertcenter.AlertStatus
import com.vordain.guard.core.alertcenter.AlertType
import com.vordain.guard.core.alertcenter.ChildAlert
import com.vordain.guard.core.alertcenter.ChildAlertTimeline
import com.vordain.guard.core.model.DeviceId
import java.nio.charset.StandardCharsets
import java.util.Base64

class ChildAlertStateStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): ChildAlertTimeline {
        val alerts = preferences.getString(KEY_ALERTS, null)
            ?.lineSequence()
            ?.mapNotNull(::decodeAlert)
            ?.toList()
            ?: emptyList()
        return ChildAlertTimeline(alerts)
    }

    fun save(timeline: ChildAlertTimeline) {
        preferences.edit()
            .putString(KEY_ALERTS, timeline.alerts.take(MAX_ALERTS).joinToString(separator = "\n", transform = ::encodeAlert))
            .apply()
    }

    private fun encodeAlert(alert: ChildAlert): String {
        return listOf(
            encode(alert.id),
            alert.type.name,
            alert.severity.name,
            alert.status.name,
            encode(alert.childDeviceId.value),
            alert.occurredAtMillis.toString(),
            encode(alert.title),
            encode(alert.detail),
            encode(alert.sourceLabel),
            encode(alert.policyVersion.orEmpty()),
        ).joinToString(separator = "|")
    }

    private fun decodeAlert(line: String): ChildAlert? {
        val parts = line.split('|')
        if (parts.size != FIELD_COUNT) {
            return null
        }
        return runCatching {
            ChildAlert(
                id = decode(parts[0]),
                type = AlertType.valueOf(parts[1]),
                severity = AlertSeverity.valueOf(parts[2]),
                status = AlertStatus.valueOf(parts[3]),
                childDeviceId = DeviceId(decode(parts[4])),
                occurredAtMillis = parts[5].toLong(),
                title = decode(parts[6]),
                detail = decode(parts[7]),
                sourceLabel = decode(parts[8]),
                policyVersion = decode(parts[9]).takeIf(String::isNotBlank),
            )
        }.getOrNull()
    }

    private fun encode(value: String): String {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decode(value: String): String {
        return String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    }

    companion object {
        private const val PREFERENCES_NAME = "vordain_child_alert_state"
        private const val KEY_ALERTS = "alerts"
        private const val FIELD_COUNT = 10
        const val MAX_ALERTS = 50
    }
}
