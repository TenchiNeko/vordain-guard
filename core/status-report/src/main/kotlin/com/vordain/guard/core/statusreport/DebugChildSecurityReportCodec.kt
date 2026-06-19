package com.vordain.guard.core.statusreport

import com.vordain.guard.core.model.DeviceId

sealed class DebugChildSecurityReportCodecResult {
    data class Decoded(val report: ChildSecurityStatusReport) : DebugChildSecurityReportCodecResult()
    data class Rejected(val reason: String) : DebugChildSecurityReportCodecResult()
}

class DebugChildSecurityReportCodec {
    fun encode(report: ChildSecurityStatusReport): String {
        return listOf(
            HEADER,
            "childDeviceId=${report.childDeviceId.value}",
            "generatedAtMillis=${report.generatedAtMillis}",
            "overallStatus=${report.overallStatus.name}",
            "policyVersion=${report.policyVersion.orEmpty()}",
            "vpnSessionLabel=${report.vpnSessionLabel.orEmpty()}",
            "heartbeatLabel=${report.heartbeatLabel.orEmpty()}",
            "setupSummaryLabel=${report.setupSummaryLabel.orEmpty()}",
            "bypassRiskLabel=${report.bypassRiskLabel.orEmpty()}",
            "activePolicySource=${report.activePolicySource.orEmpty()}",
            "activePolicyPreset=${report.activePolicyPreset.orEmpty()}",
            "encryptedDnsBlockingEnabled=${report.encryptedDnsBlockingEnabled}",
            "proxyBlockingEnabled=${report.proxyBlockingEnabled}",
            "policyAllowDomainCount=${report.policyAllowDomainCount}",
            "policyBlockDomainCount=${report.policyBlockDomainCount}",
            "activeMode=${report.activeMode.name}",
            "dnsBlockedResponseCount=${report.dnsBlockedResponseCount}",
            "dnsAllowedForwardedCount=${report.dnsAllowedForwardedCount}",
            "dnsAllowedForwardFailureCount=${report.dnsAllowedForwardFailureCount}",
            "signals=${report.signals.map(ChildSecuritySignal::name).sorted().joinToString(separator = ",")}",
            "warning=${report.warningText}",
        ).joinToString(separator = "\n")
    }

    fun decode(payload: String): DebugChildSecurityReportCodecResult {
        val lines = payload.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
        if (lines.firstOrNull() != HEADER) {
            return DebugChildSecurityReportCodecResult.Rejected("Wrong debug child security report header")
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
            return DebugChildSecurityReportCodecResult.Rejected("Forbidden sensitive field: $forbiddenField")
        }
        val childDeviceId = values["childDeviceId"]?.takeIf(String::isNotBlank)
            ?: return DebugChildSecurityReportCodecResult.Rejected("Missing childDeviceId")
        val generatedAtMillis = values["generatedAtMillis"]?.toLongOrNull()
            ?: return DebugChildSecurityReportCodecResult.Rejected("Malformed generatedAtMillis")
        val status = values["overallStatus"]?.let { encodedStatus ->
            enumValueOrNull<ChildSecurityOverallStatus>(encodedStatus)
        } ?: return DebugChildSecurityReportCodecResult.Rejected("Unknown overallStatus")
        val signals = values["signals"].orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { encodedSignal ->
                enumValueOrNull<ChildSecuritySignal>(encodedSignal)
                    ?: return DebugChildSecurityReportCodecResult.Rejected("Unknown signal")
            }
            .toSet()
        val activeMode = values["activeMode"]?.let { encodedMode ->
            enumValueOrNull<ChildSecurityActiveMode>(encodedMode)
        } ?: ChildSecurityActiveMode.NONE
        return DebugChildSecurityReportCodecResult.Decoded(
            ChildSecurityStatusReport(
                childDeviceId = DeviceId(childDeviceId),
                generatedAtMillis = generatedAtMillis,
                overallStatus = status,
                signals = signals,
                policyVersion = values["policyVersion"]?.takeIf(String::isNotBlank),
                vpnSessionLabel = values["vpnSessionLabel"]?.takeIf(String::isNotBlank),
                heartbeatLabel = values["heartbeatLabel"]?.takeIf(String::isNotBlank),
                setupSummaryLabel = values["setupSummaryLabel"]?.takeIf(String::isNotBlank),
                bypassRiskLabel = values["bypassRiskLabel"]?.takeIf(String::isNotBlank),
                activePolicySource = values["activePolicySource"]?.takeIf(String::isNotBlank),
                activePolicyPreset = values["activePolicyPreset"]?.takeIf(String::isNotBlank),
                encryptedDnsBlockingEnabled = values["encryptedDnsBlockingEnabled"].toBooleanOrDefault(defaultValue = true),
                proxyBlockingEnabled = values["proxyBlockingEnabled"].toBooleanOrDefault(defaultValue = true),
                policyAllowDomainCount = values["policyAllowDomainCount"]?.toIntOrNull() ?: 0,
                policyBlockDomainCount = values["policyBlockDomainCount"]?.toIntOrNull() ?: 0,
                activeMode = activeMode,
                dnsBlockedResponseCount = values["dnsBlockedResponseCount"]?.toLongOrNull() ?: 0,
                dnsAllowedForwardedCount = values["dnsAllowedForwardedCount"]?.toLongOrNull() ?: 0,
                dnsAllowedForwardFailureCount = values["dnsAllowedForwardFailureCount"]?.toLongOrNull() ?: 0,
                warningText = values["warning"]?.takeIf(String::isNotBlank)
                    ?: ChildSecurityStatusReport.WARNING_TEXT,
            ),
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? {
        return enumValues<T>().firstOrNull { enumValue -> enumValue.name == value }
    }

    private fun String?.toBooleanOrDefault(defaultValue: Boolean): Boolean {
        return when (this?.trim()?.lowercase()) {
            "true" -> true
            "false" -> false
            null, "" -> defaultValue
            else -> defaultValue
        }
    }

    companion object {
        const val HEADER = "VORDAIN_DEBUG_CHILD_SECURITY_REPORT_V1"
        private val forbiddenFieldNames = setOf(
            "pin" + "Value",
            "pin" + "Digits",
            "password",
            "key" + "strokes",
            "credential",
        )
    }
}
