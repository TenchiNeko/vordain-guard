package com.vordain.guard.core.statusreport

import com.vordain.guard.core.model.DeviceId

sealed class DebugBasicDnsGuardDiagnosticsCodecResult {
    data class Decoded(val report: BasicDnsGuardDiagnosticsReport) : DebugBasicDnsGuardDiagnosticsCodecResult()
    data class Rejected(val reason: String) : DebugBasicDnsGuardDiagnosticsCodecResult()
}

class DebugBasicDnsGuardDiagnosticsCodec {
    fun encode(report: BasicDnsGuardDiagnosticsReport): String {
        return listOf(
            HEADER,
            "childDeviceId=${report.childDeviceId.value}",
            "generatedAtMillis=${report.generatedAtMillis}",
            "mode=${report.mode.name}",
            "activePolicySource=${report.activePolicySource}",
            "activePolicyVersion=${report.activePolicyVersion.orEmpty()}",
            "activePreset=${report.activePreset.orEmpty()}",
            "readinessStatus=${report.readinessStatus}",
            "hardeningSummary=${report.hardeningSummary}",
            "bypassRiskSummary=${report.bypassRiskSummary}",
            "dnsBlockedCount=${report.dnsBlockedCount}",
            "dnsAllowedForwardedCount=${report.dnsAllowedForwardedCount}",
            "encryptedDnsBlockedCount=${report.encryptedDnsBlockedCount}",
            "dnsFailureCount=${report.dnsFailureCount}",
            "warning=${report.warningText}",
        ).joinToString(separator = "\n")
    }

    fun decode(payload: String): DebugBasicDnsGuardDiagnosticsCodecResult {
        val lines = payload.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
        if (lines.firstOrNull() != HEADER) {
            return DebugBasicDnsGuardDiagnosticsCodecResult.Rejected("Wrong Basic DNS Guard diagnostics header")
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
            return DebugBasicDnsGuardDiagnosticsCodecResult.Rejected("Forbidden sensitive field: $forbiddenField")
        }
        val childDeviceId = values["childDeviceId"]?.takeIf(String::isNotBlank)
            ?: return DebugBasicDnsGuardDiagnosticsCodecResult.Rejected("Missing childDeviceId")
        val generatedAtMillis = values["generatedAtMillis"]?.toLongOrNull()
            ?: return DebugBasicDnsGuardDiagnosticsCodecResult.Rejected("Malformed generatedAtMillis")
        val mode = values["mode"]?.let { encodedMode ->
            enumValues<ChildSecurityActiveMode>().firstOrNull { mode -> mode.name == encodedMode }
        } ?: return DebugBasicDnsGuardDiagnosticsCodecResult.Rejected("Unknown mode")
        if (mode != ChildSecurityActiveMode.BASIC_DNS_GUARD) {
            return DebugBasicDnsGuardDiagnosticsCodecResult.Rejected("Diagnostics mode must be BASIC_DNS_GUARD")
        }
        return DebugBasicDnsGuardDiagnosticsCodecResult.Decoded(
            BasicDnsGuardDiagnosticsReport(
                childDeviceId = DeviceId(childDeviceId),
                generatedAtMillis = generatedAtMillis,
                mode = mode,
                activePolicySource = values["activePolicySource"].orEmpty(),
                activePolicyVersion = values["activePolicyVersion"]?.takeIf(String::isNotBlank),
                activePreset = values["activePreset"]?.takeIf(String::isNotBlank),
                readinessStatus = values["readinessStatus"].orEmpty(),
                hardeningSummary = values["hardeningSummary"].orEmpty(),
                bypassRiskSummary = values["bypassRiskSummary"].orEmpty(),
                dnsBlockedCount = values["dnsBlockedCount"]?.toLongOrNull() ?: 0,
                dnsAllowedForwardedCount = values["dnsAllowedForwardedCount"]?.toLongOrNull() ?: 0,
                encryptedDnsBlockedCount = values["encryptedDnsBlockedCount"]?.toLongOrNull() ?: 0,
                dnsFailureCount = values["dnsFailureCount"]?.toLongOrNull() ?: 0,
                warningText = values["warning"]?.takeIf(String::isNotBlank)
                    ?: BasicDnsGuardDiagnosticsReport.WARNING_TEXT,
            ),
        )
    }

    companion object {
        const val HEADER = "VORDAIN_DEBUG_BASIC_DNS_GUARD_DIAGNOSTICS_V1"
        private val forbiddenFieldNames = setOf(
            "pin" + "Value",
            "pin" + "Digits",
            "password",
            "key" + "strokes",
            "credential",
        )
    }
}
