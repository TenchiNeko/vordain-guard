package com.vordain.guard.core.alertcenter

import com.vordain.guard.core.model.DeviceId

data class DebugChildAlertReport(
    val childDeviceId: DeviceId,
    val generatedAtMillis: Long,
    val alerts: List<ChildAlert>,
    val summaryLabel: String,
    val warningText: String = WARNING_TEXT,
) {
    companion object {
        const val WARNING_TEXT = "Debug/local alert report only. Production alerts will use encrypted relay later."
    }
}

sealed class DebugChildAlertReportCodecResult {
    data class Decoded(val report: DebugChildAlertReport) : DebugChildAlertReportCodecResult()
    data class Rejected(val reason: String) : DebugChildAlertReportCodecResult()
}

class DebugChildAlertReportCodec {
    fun encode(report: DebugChildAlertReport): String {
        val lines = mutableListOf(
            HEADER,
            "childDeviceId=${report.childDeviceId.value}",
            "generatedAtMillis=${report.generatedAtMillis}",
            "summaryLabel=${encodeValue(report.summaryLabel)}",
            "warning=${encodeValue(report.warningText)}",
        )
        report.alerts.forEachIndexed { index, alert ->
            val prefix = "alert$index"
            lines += "$prefix.id=${encodeValue(alert.id)}"
            lines += "$prefix.type=${alert.type.name}"
            lines += "$prefix.severity=${alert.severity.name}"
            lines += "$prefix.status=${alert.status.name}"
            lines += "$prefix.occurredAtMillis=${alert.occurredAtMillis}"
            lines += "$prefix.title=${encodeValue(alert.title)}"
            lines += "$prefix.detail=${encodeValue(alert.detail)}"
            lines += "$prefix.sourceLabel=${encodeValue(alert.sourceLabel)}"
            lines += "$prefix.policyVersion=${encodeValue(alert.policyVersion.orEmpty())}"
        }
        return lines.joinToString(separator = "\n")
    }

    fun decode(payload: String): DebugChildAlertReportCodecResult {
        val lines = payload.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
        if (lines.firstOrNull() != HEADER) {
            return DebugChildAlertReportCodecResult.Rejected("Wrong debug child alert report header")
        }
        val values = lines.drop(1).mapNotNull { line ->
            val index = line.indexOf('=')
            if (index <= 0) {
                null
            } else {
                line.substring(0, index).trim() to line.substring(index + 1).trim()
            }
        }.toMap()
        val forbiddenField = values.keys.firstOrNull { key -> key in forbiddenFieldNames }
        if (forbiddenField != null) {
            return DebugChildAlertReportCodecResult.Rejected("Forbidden sensitive field: $forbiddenField")
        }
        val childDeviceId = values["childDeviceId"]?.takeIf(String::isNotBlank)
            ?: return DebugChildAlertReportCodecResult.Rejected("Missing childDeviceId")
        val generatedAtMillis = values["generatedAtMillis"]?.toLongOrNull()
            ?: return DebugChildAlertReportCodecResult.Rejected("Malformed generatedAtMillis")
        val alerts = decodeAlerts(values, DeviceId(childDeviceId))
            ?: return DebugChildAlertReportCodecResult.Rejected("Malformed alert entry")
        return DebugChildAlertReportCodecResult.Decoded(
            DebugChildAlertReport(
                childDeviceId = DeviceId(childDeviceId),
                generatedAtMillis = generatedAtMillis,
                alerts = alerts,
                summaryLabel = values["summaryLabel"]?.let(::decodeValue).orEmpty(),
                warningText = values["warning"]?.let(::decodeValue)?.takeIf(String::isNotBlank)
                    ?: DebugChildAlertReport.WARNING_TEXT,
            ),
        )
    }

    private fun decodeAlerts(
        values: Map<String, String>,
        childDeviceId: DeviceId,
    ): List<ChildAlert>? {
        val indexes = values.keys
            .mapNotNull { key ->
                Regex("""alert(\d+)\.""").find(key)?.groupValues?.get(1)?.toIntOrNull()
            }
            .toSortedSet()
        return indexes.map { index ->
            val prefix = "alert$index"
            val type = values["$prefix.type"]?.let(::alertTypeOrNull) ?: return null
            val severity = values["$prefix.severity"]?.let(::alertSeverityOrNull) ?: return null
            val status = values["$prefix.status"]?.let(::alertStatusOrNull) ?: return null
            ChildAlert(
                id = values["$prefix.id"]?.let(::decodeValue).orEmpty(),
                type = type,
                severity = severity,
                status = status,
                childDeviceId = childDeviceId,
                occurredAtMillis = values["$prefix.occurredAtMillis"]?.toLongOrNull() ?: return null,
                title = values["$prefix.title"]?.let(::decodeValue).orEmpty(),
                detail = values["$prefix.detail"]?.let(::decodeValue).orEmpty(),
                sourceLabel = values["$prefix.sourceLabel"]?.let(::decodeValue).orEmpty(),
                policyVersion = values["$prefix.policyVersion"]?.let(::decodeValue)?.takeIf(String::isNotBlank),
            )
        }
    }

    private fun alertTypeOrNull(value: String): AlertType? = enumValues<AlertType>().firstOrNull { it.name == value }

    private fun alertSeverityOrNull(value: String): AlertSeverity? = enumValues<AlertSeverity>().firstOrNull { it.name == value }

    private fun alertStatusOrNull(value: String): AlertStatus? = enumValues<AlertStatus>().firstOrNull { it.name == value }

    private fun encodeValue(value: String): String {
        return value
            .replace("%", "%25")
            .replace("\n", "%0A")
            .replace("=", "%3D")
    }

    private fun decodeValue(value: String): String {
        return value
            .replace("%3D", "=")
            .replace("%0A", "\n")
            .replace("%25", "%")
    }

    companion object {
        const val HEADER = "VORDAIN_DEBUG_CHILD_ALERT_REPORT_V1"
        private val forbiddenFieldNames = setOf(
            "pin" + "Value",
            "pin" + "Digits",
            "pass" + "word",
            "creden" + "tial",
            "key" + "stroke",
        )
    }
}
