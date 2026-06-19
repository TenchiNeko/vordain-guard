package com.vordain.guard.features.setupchecklist

import com.vordain.guard.core.model.DeviceId

enum class HardeningSetupStep {
    VPN_PERMISSION,
    VPN_ALWAYS_ON,
    BLOCK_WITHOUT_VPN,
    SETTINGS_APP_LOCK,
    SCREEN_PINNING_WITH_PIN,
    BATTERY_OPTIMIZATION,
    PRIVATE_DNS_REVIEW,
    UNKNOWN_SOURCES_REVIEW,
    FINAL_PARENT_REVIEW,
}

enum class HardeningSetupStatus {
    NOT_STARTED,
    OPENED_SETTINGS,
    USER_CONFIRMED,
    AUTO_CONFIRMED,
    NEEDS_ATTENTION,
    NOT_SUPPORTED,
    UNKNOWN,
}

enum class HardeningEvidenceType {
    AUTOMATIC_CHECK,
    PARENT_CONFIRMATION,
    MANUAL_DEVICE_SETTING,
    BEHAVIOR_TEST,
    UNKNOWN,
}

enum class HardeningSummaryStatus {
    NOT_STARTED,
    IN_PROGRESS,
    READY_FOR_LAB_TEST,
    NEEDS_ATTENTION,
    UNKNOWN,
}

data class HardeningSetupItem(
    val step: HardeningSetupStep,
    val status: HardeningSetupStatus,
    val evidenceType: HardeningEvidenceType,
    val updatedAtMillis: Long,
    val note: String? = null,
)

data class HardeningSetupSnapshot(
    val childDeviceId: DeviceId,
    val generatedAtMillis: Long,
    val items: List<HardeningSetupItem>,
    val summaryStatus: HardeningSummaryStatus,
    val warningText: String = DEFAULT_WARNING_TEXT,
) {
    fun itemFor(step: HardeningSetupStep): HardeningSetupItem {
        return items.firstOrNull { item -> item.step == step }
            ?: HardeningSetupItem(
                step = step,
                status = HardeningSetupStatus.NOT_STARTED,
                evidenceType = HardeningEvidenceType.UNKNOWN,
                updatedAtMillis = generatedAtMillis,
            )
    }

    companion object {
        const val DEFAULT_WARNING_TEXT =
            "Setup confirmed means parent-confirmed device settings; filtering is not enabled yet."
    }
}

class HardeningSetupReducer {
    fun initialSnapshot(
        childDeviceId: DeviceId,
        currentTimeMillis: Long,
    ): HardeningSetupSnapshot {
        return HardeningSetupSnapshot(
            childDeviceId = childDeviceId,
            generatedAtMillis = currentTimeMillis,
            items = HardeningSetupStep.entries.map { step ->
                HardeningSetupItem(
                    step = step,
                    status = HardeningSetupStatus.NOT_STARTED,
                    evidenceType = HardeningEvidenceType.UNKNOWN,
                    updatedAtMillis = currentTimeMillis,
                )
            },
            summaryStatus = HardeningSummaryStatus.NOT_STARTED,
        )
    }

    fun updateStep(
        snapshot: HardeningSetupSnapshot,
        step: HardeningSetupStep,
        status: HardeningSetupStatus,
        evidenceType: HardeningEvidenceType,
        note: String?,
        currentTimeMillis: Long,
    ): HardeningSetupSnapshot {
        val updatedItem = HardeningSetupItem(
            step = step,
            status = status,
            evidenceType = evidenceType,
            updatedAtMillis = currentTimeMillis,
            note = note?.trim()?.takeIf(String::isNotEmpty),
        )
        val updatedItems = HardeningSetupStep.entries.map { knownStep ->
            if (knownStep == step) {
                updatedItem
            } else {
                snapshot.itemFor(knownStep)
            }
        }
        val updatedSnapshot = snapshot.copy(
            generatedAtMillis = currentTimeMillis,
            items = updatedItems,
        )
        return updatedSnapshot.copy(summaryStatus = summarize(updatedSnapshot))
    }

    fun summarize(snapshot: HardeningSetupSnapshot): HardeningSummaryStatus {
        val items = HardeningSetupStep.entries.associateWith(snapshot::itemFor)
        if (items.values.all { item -> item.status == HardeningSetupStatus.NOT_STARTED }) {
            return HardeningSummaryStatus.NOT_STARTED
        }
        if (items.values.any { item -> item.status == HardeningSetupStatus.NEEDS_ATTENTION }) {
            return HardeningSummaryStatus.NEEDS_ATTENTION
        }
        val criticalSteps = listOf(
            HardeningSetupStep.VPN_PERMISSION,
            HardeningSetupStep.VPN_ALWAYS_ON,
            HardeningSetupStep.BLOCK_WITHOUT_VPN,
        )
        val criticalReady = criticalSteps.all { step ->
            items.getValue(step).status.isConfirmed()
        }
        if (!criticalReady) {
            return HardeningSummaryStatus.NEEDS_ATTENTION
        }
        return HardeningSummaryStatus.READY_FOR_LAB_TEST
    }

    private fun HardeningSetupStatus.isConfirmed(): Boolean {
        return this == HardeningSetupStatus.USER_CONFIRMED || this == HardeningSetupStatus.AUTO_CONFIRMED
    }
}
