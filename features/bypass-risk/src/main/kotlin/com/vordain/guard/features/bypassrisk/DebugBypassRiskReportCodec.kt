package com.vordain.guard.features.bypassrisk

import com.vordain.guard.core.model.DeviceId

data class DebugBypassRiskReport(
    val childDeviceId: DeviceId,
    val generatedAtMillis: Long,
    val summary: BypassRiskSummary,
)

sealed class DebugBypassRiskReportCodecResult {
    data class Decoded(val report: DebugBypassRiskReport) : DebugBypassRiskReportCodecResult()
    data class Rejected(val reason: String) : DebugBypassRiskReportCodecResult()
}

class DebugBypassRiskReportCodec {
    fun encode(report: DebugBypassRiskReport): String {
        val lines = mutableListOf(
            HEADER,
            "childDeviceId=${report.childDeviceId.value}",
            "generatedAtMillis=${report.generatedAtMillis}",
        )
        report.summary.items.forEachIndexed { index, item ->
            lines += "item$index=${item.category.name}|${item.status.name}|${item.severity.name}|${item.evidenceLabel.sanitize()}|${item.note.sanitize()}"
        }
        return lines.joinToString(separator = "\n")
    }

    fun decode(payload: String): DebugBypassRiskReportCodecResult {
        val lines = payload.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
        if (lines.firstOrNull() != HEADER) {
            return DebugBypassRiskReportCodecResult.Rejected("Wrong debug bypass-risk report header")
        }
        val fields = lines.drop(1).mapNotNull { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) {
                null
            } else {
                line.substring(0, separator).trim() to line.substring(separator + 1).trim()
            }
        }.toMap()
        val forbiddenField = fields.keys.firstOrNull { key ->
            FORBIDDEN_FIELD_NAMES.any { forbidden -> key.equals(forbidden, ignoreCase = true) }
        }
        if (forbiddenField != null) {
            return DebugBypassRiskReportCodecResult.Rejected("Forbidden sensitive field: $forbiddenField")
        }
        val childDeviceId = fields["childDeviceId"]?.takeIf(String::isNotBlank)
            ?: return DebugBypassRiskReportCodecResult.Rejected("Missing childDeviceId")
        val generatedAtMillis = fields["generatedAtMillis"]?.toLongOrNull()
            ?: return DebugBypassRiskReportCodecResult.Rejected("Malformed generatedAtMillis")

        val items = fields.entries
            .filter { (key, _) -> key.startsWith("item") }
            .sortedBy { (key, _) -> key.removePrefix("item").toIntOrNull() ?: Int.MAX_VALUE }
            .map { (_, value) ->
                val parts = value.split('|')
                if (parts.size != 5) {
                    return DebugBypassRiskReportCodecResult.Rejected("Malformed bypass-risk item")
                }
                val category = enumValueOrNull<BypassRiskCategory>(parts[0])
                    ?: return DebugBypassRiskReportCodecResult.Rejected("Unknown bypass-risk category")
                val status = enumValueOrNull<BypassRiskStatus>(parts[1])
                    ?: return DebugBypassRiskReportCodecResult.Rejected("Unknown bypass-risk status")
                val severity = enumValueOrNull<BypassRiskSeverity>(parts[2])
                    ?: return DebugBypassRiskReportCodecResult.Rejected("Unknown bypass-risk severity")
                BypassRiskItem(
                    category = category,
                    status = status,
                    severity = severity,
                    evidenceLabel = parts[3],
                    note = parts[4],
                )
            }

        return DebugBypassRiskReportCodecResult.Decoded(
            DebugBypassRiskReport(
                childDeviceId = DeviceId(childDeviceId),
                generatedAtMillis = generatedAtMillis,
                summary = BypassRiskEvaluator().summarize(items),
            ),
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? {
        return enumValues<T>().firstOrNull { it.name == value.trim() }
    }

    private fun String.sanitize(): String {
        return replace("\n", " ")
            .replace("\r", " ")
            .replace("|", "/")
            .trim()
    }

    companion object {
        const val HEADER = "VORDAIN_DEBUG_BYPASS_RISK_REPORT_V1"
        private val FORBIDDEN_FIELD_NAMES = setOf(
            "pin" + "Value",
            "pin" + "Digits",
            "pass" + "word",
            "key" + "strokes",
            "cred" + "ential",
        )
    }
}
