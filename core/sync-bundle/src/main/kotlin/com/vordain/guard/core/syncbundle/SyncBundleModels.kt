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

enum class SyncBundleImportTarget {
    PARENT_APP,
    CHILD_APP,
}

enum class SyncBundleImportStatus {
    ACCEPTED,
    REJECTED,
    WRONG_DIRECTION,
    MISSING_REQUIRED_PAYLOAD,
    MALFORMED,
    UNSUPPORTED_KIND,
}

data class SyncBundleImportResult(
    val status: SyncBundleImportStatus,
    val target: SyncBundleImportTarget,
    val bundleId: String? = null,
    val direction: SyncBundleDirection? = null,
    val sourceDeviceId: DeviceId? = null,
    val targetDeviceId: DeviceId? = null,
    val summary: String,
    val payloadLabels: List<String> = emptyList(),
)

class SyncBundleImportEvaluator {
    fun evaluateForParent(bundle: SyncBundle): SyncBundleImportResult {
        if (bundle.direction != SyncBundleDirection.CHILD_TO_PARENT) {
            return result(bundle, SyncBundleImportTarget.PARENT_APP, SyncBundleImportStatus.WRONG_DIRECTION)
        }
        return result(bundle, SyncBundleImportTarget.PARENT_APP, SyncBundleImportStatus.ACCEPTED)
    }

    fun evaluateForChild(bundle: SyncBundle): SyncBundleImportResult {
        if (bundle.direction != SyncBundleDirection.PARENT_TO_CHILD) {
            return result(bundle, SyncBundleImportTarget.CHILD_APP, SyncBundleImportStatus.WRONG_DIRECTION)
        }
        if (bundle.payloads.none { it.kind == SyncBundlePayloadKind.POLICY_UPDATE }) {
            return result(
                bundle = bundle,
                target = SyncBundleImportTarget.CHILD_APP,
                status = SyncBundleImportStatus.MISSING_REQUIRED_PAYLOAD,
                summary = "Missing policy update payload",
            )
        }
        return result(bundle, SyncBundleImportTarget.CHILD_APP, SyncBundleImportStatus.ACCEPTED)
    }

    fun malformedForParent(reason: String): SyncBundleImportResult {
        return SyncBundleImportResult(
            status = SyncBundleImportStatus.MALFORMED,
            target = SyncBundleImportTarget.PARENT_APP,
            summary = reason,
        )
    }

    fun malformedForChild(reason: String): SyncBundleImportResult {
        return SyncBundleImportResult(
            status = SyncBundleImportStatus.MALFORMED,
            target = SyncBundleImportTarget.CHILD_APP,
            summary = reason,
        )
    }

    private fun result(
        bundle: SyncBundle,
        target: SyncBundleImportTarget,
        status: SyncBundleImportStatus,
        summary: String = status.toSummary(),
    ): SyncBundleImportResult {
        return SyncBundleImportResult(
            status = status,
            target = target,
            bundleId = bundle.bundleId,
            direction = bundle.direction,
            sourceDeviceId = bundle.sourceDeviceId,
            targetDeviceId = bundle.targetDeviceId,
            summary = summary,
            payloadLabels = bundle.payloads.map { payload ->
                "${payload.kind.name}: ${payload.label.ifBlank { "unlabeled" }}"
            },
        )
    }

    private fun SyncBundleImportStatus.toSummary(): String {
        return when (this) {
            SyncBundleImportStatus.ACCEPTED -> "Local sync bundle accepted"
            SyncBundleImportStatus.REJECTED -> "Local sync bundle rejected"
            SyncBundleImportStatus.WRONG_DIRECTION -> "Wrong bundle direction for this app"
            SyncBundleImportStatus.MISSING_REQUIRED_PAYLOAD -> "Missing required payload"
            SyncBundleImportStatus.MALFORMED -> "Malformed local sync bundle"
            SyncBundleImportStatus.UNSUPPORTED_KIND -> "Unsupported local sync bundle kind"
        }
    }
}

enum class MvpAcceptanceStep {
    PARENT_BUILDS_POLICY,
    PARENT_EXPORTS_SYNC_BUNDLE,
    CHILD_IMPORTS_SYNC_BUNDLE,
    CHILD_APPLIES_VERIFIED_POLICY,
    CHILD_COMPLETES_HARDENING_REVIEW,
    CHILD_STARTS_BASIC_DNS_GUARD,
    CHILD_EXPORTS_STATUS_BUNDLE,
    PARENT_IMPORTS_STATUS_BUNDLE,
    PARENT_REVIEWS_ALERTS,
    DEV_RELAY_RUNNING,
    PARENT_SENT_POLICY_VIA_RELAY,
    CHILD_FETCHED_POLICY_FROM_RELAY,
    CHILD_SENT_STATUS_VIA_RELAY,
    PARENT_FETCHED_STATUS_FROM_RELAY,
}

enum class MvpAcceptanceStatus {
    NOT_STARTED,
    DONE,
    NEEDS_ATTENTION,
    SKIPPED,
}

data class MvpAcceptanceItem(
    val step: MvpAcceptanceStep,
    val status: MvpAcceptanceStatus,
)

data class MvpAcceptanceSummary(
    val status: MvpAcceptanceStatus,
    val completedCount: Int,
    val totalCount: Int,
    val nextRecommendedStep: MvpAcceptanceStep?,
)

class MvpAcceptanceChecklist {
    fun summarize(items: List<MvpAcceptanceItem>): MvpAcceptanceSummary {
        val byStep = items.associateBy(MvpAcceptanceItem::step)
        val completedCount = MvpAcceptanceStep.entries.count { step ->
            byStep[step]?.status == MvpAcceptanceStatus.DONE
        }
        val nextAttention = MvpAcceptanceStep.entries.firstOrNull { step ->
            byStep[step]?.status == MvpAcceptanceStatus.NEEDS_ATTENTION
        }
        val nextIncomplete = MvpAcceptanceStep.entries.firstOrNull { step ->
            val status = byStep[step]?.status ?: MvpAcceptanceStatus.NOT_STARTED
            status == MvpAcceptanceStatus.NOT_STARTED || status == MvpAcceptanceStatus.NEEDS_ATTENTION
        }
        val next = nextAttention ?: nextIncomplete
        return MvpAcceptanceSummary(
            status = when {
                nextAttention != null -> MvpAcceptanceStatus.NEEDS_ATTENTION
                completedCount == MvpAcceptanceStep.entries.size -> MvpAcceptanceStatus.DONE
                completedCount == 0 -> MvpAcceptanceStatus.NOT_STARTED
                else -> MvpAcceptanceStatus.NOT_STARTED
            },
            completedCount = completedCount,
            totalCount = MvpAcceptanceStep.entries.size,
            nextRecommendedStep = next,
        )
    }
}
