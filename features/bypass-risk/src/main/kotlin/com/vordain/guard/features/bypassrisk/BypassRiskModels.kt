package com.vordain.guard.features.bypassrisk

enum class BypassRiskCategory {
    DNS_OVER_HTTPS,
    PRIVATE_DNS,
    ALTERNATE_VPN_APP,
    PROXY_APP,
    PRIVATE_BROWSER,
    DEVELOPER_OPTIONS,
    USB_DEBUGGING,
    WIRELESS_DEBUGGING,
    UNRESTRICTED_PROFILE,
    DIRECT_IP_ACCESS,
    UNKNOWN,
}

enum class BypassRiskSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

enum class BypassRiskStatus {
    NOT_CHECKED,
    NEEDS_REVIEW,
    CONFIRMED_SAFE,
    RISK_FOUND,
    NOT_SUPPORTED,
    UNKNOWN,
}

data class BypassRiskItem(
    val category: BypassRiskCategory,
    val status: BypassRiskStatus,
    val severity: BypassRiskSeverity,
    val evidenceLabel: String,
    val note: String,
)

enum class BypassRiskOverallStatus {
    NOT_REVIEWED,
    READY_FOR_DNS_LAB,
    NEEDS_ATTENTION,
    HIGH_RISK,
    UNKNOWN,
}

data class BypassRiskSummary(
    val overallStatus: BypassRiskOverallStatus,
    val highestSeverity: BypassRiskSeverity,
    val items: List<BypassRiskItem>,
    val warningText: String = WARNING_TEXT,
) {
    companion object {
        const val WARNING_TEXT = "DNS-only filtering is not full protection; DoH, direct-IP, app VPN, and proxy paths require hardening."
    }
}

class BypassRiskEvaluator {
    fun summarize(items: List<BypassRiskItem>): BypassRiskSummary {
        if (items.isEmpty()) {
            return BypassRiskSummary(
                overallStatus = BypassRiskOverallStatus.NOT_REVIEWED,
                highestSeverity = BypassRiskSeverity.INFO,
                items = emptyList(),
            )
        }

        val highestSeverity = items.maxByOrNull { it.severity.ordinal }?.severity ?: BypassRiskSeverity.INFO
        val highRiskFound = items.any { item ->
            item.status == BypassRiskStatus.RISK_FOUND && (
                item.severity.ordinal >= BypassRiskSeverity.HIGH.ordinal ||
                    item.category in HIGH_RISK_IF_FOUND_CATEGORIES
                )
        }
        val requiredNeedsReview = items.any { item ->
            item.category in REQUIRED_DNS_LAB_REVIEW_CATEGORIES &&
                item.status != BypassRiskStatus.CONFIRMED_SAFE &&
                item.status != BypassRiskStatus.NOT_SUPPORTED
        }
        val unknown = items.any { it.status == BypassRiskStatus.UNKNOWN }
        val allRequiredSafe = REQUIRED_DNS_LAB_REVIEW_CATEGORIES.all { requiredCategory ->
            items.any { item ->
                item.category == requiredCategory &&
                    (item.status == BypassRiskStatus.CONFIRMED_SAFE || item.status == BypassRiskStatus.NOT_SUPPORTED)
            }
        }

        val overallStatus = when {
            highRiskFound -> BypassRiskOverallStatus.HIGH_RISK
            requiredNeedsReview -> BypassRiskOverallStatus.NEEDS_ATTENTION
            allRequiredSafe -> BypassRiskOverallStatus.READY_FOR_DNS_LAB
            unknown -> BypassRiskOverallStatus.UNKNOWN
            else -> BypassRiskOverallStatus.NEEDS_ATTENTION
        }

        return BypassRiskSummary(
            overallStatus = overallStatus,
            highestSeverity = highestSeverity,
            items = items,
        )
    }

    companion object {
        val REQUIRED_DNS_LAB_REVIEW_CATEGORIES = setOf(
            BypassRiskCategory.DNS_OVER_HTTPS,
            BypassRiskCategory.PRIVATE_DNS,
            BypassRiskCategory.ALTERNATE_VPN_APP,
            BypassRiskCategory.PROXY_APP,
            BypassRiskCategory.PRIVATE_BROWSER,
            BypassRiskCategory.DEVELOPER_OPTIONS,
            BypassRiskCategory.USB_DEBUGGING,
            BypassRiskCategory.WIRELESS_DEBUGGING,
            BypassRiskCategory.UNRESTRICTED_PROFILE,
        )
        private val HIGH_RISK_IF_FOUND_CATEGORIES = setOf(
            BypassRiskCategory.DNS_OVER_HTTPS,
            BypassRiskCategory.ALTERNATE_VPN_APP,
            BypassRiskCategory.PROXY_APP,
            BypassRiskCategory.PRIVATE_BROWSER,
        )
    }
}
