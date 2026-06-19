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
    UNKNOWN_SOURCES_REVIEWED,
    DEVELOPER_OPTIONS_DISABLED,
    USB_DEBUGGING_DISABLED,
    WIRELESS_DEBUGGING_DISABLED,
    NO_UNRESTRICTED_SECONDARY_USERS,
    NO_UNRESTRICTED_WORK_PROFILE,
    SETTINGS_LOCK_PARENT_PIN_ONLY,
    PARENT_PIN_NOT_SHARED,
    PARENT_MAINTENANCE_WINDOW,
    PIN_COMPROMISE_REVIEW,
    VPN_LIFECYCLE_HEALTH,
    FINAL_PARENT_REVIEW,
}

enum class HardeningSetupStatus {
    NOT_STARTED,
    OPENED_SETTINGS,
    USER_CONFIRMED,
    AUTO_CONFIRMED,
    CONFIRMED_DISABLED,
    CONFIRMED_ABSENT,
    BEST_EFFORT_AUTO_CHECK,
    PARENT_CONFIRMED,
    COMPROMISE_SUSPECTED,
    NEEDS_ATTENTION,
    NOT_SUPPORTED,
    UNKNOWN,
}

enum class HardeningEvidenceType {
    AUTOMATIC_CHECK,
    PARENT_CONFIRMATION,
    MANUAL_DEVICE_SETTING,
    MANUAL_SETTINGS_REVIEW,
    BEST_EFFORT_DEVICE_CHECK,
    STATE_CHANGE_OUTSIDE_AUTHORIZED_WINDOW,
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

enum class MaintenanceWindowReason {
    INITIAL_SETUP,
    VPN_SETTINGS_REVIEW,
    APP_LOCK_REVIEW,
    DEVELOPER_OPTIONS_REVIEW,
    PROFILE_REVIEW,
    POLICY_UPDATE,
    OTHER,
}

data class ParentMaintenanceWindow(
    val windowId: String,
    val openedAtMillis: Long,
    val expiresAtMillis: Long,
    val reason: MaintenanceWindowReason,
    val openedByParentDeviceId: DeviceId? = null,
) {
    init {
        require(windowId.isNotBlank()) { "windowId must not be blank" }
        require(expiresAtMillis > openedAtMillis) { "expiresAtMillis must be greater than openedAtMillis" }
    }

    fun isActive(currentTimeMillis: Long): Boolean {
        return currentTimeMillis >= openedAtMillis && currentTimeMillis < expiresAtMillis
    }
}

data class HardeningStateChange(
    val step: HardeningSetupStep,
    val previousStatus: HardeningSetupStatus,
    val newStatus: HardeningSetupStatus,
    val changedAtMillis: Long,
    val evidenceType: HardeningEvidenceType,
    val note: String? = null,
)

data class PinCompromiseSignal(
    val suspected: Boolean,
    val reason: String,
    val changedStep: HardeningSetupStep? = null,
    val changedAtMillis: Long? = null,
    val activeMaintenanceWindowId: String? = null,
) {
    companion object {
        val NONE = PinCompromiseSignal(
            suspected = false,
            reason = "No compromise signal",
        )
    }
}

data class HardeningSetupSnapshot(
    val childDeviceId: DeviceId,
    val generatedAtMillis: Long,
    val items: List<HardeningSetupItem>,
    val summaryStatus: HardeningSummaryStatus,
    val warningText: String = DEFAULT_WARNING_TEXT,
    val activeMaintenanceWindow: ParentMaintenanceWindow? = null,
    val latestPinCompromiseSignal: PinCompromiseSignal = PinCompromiseSignal.NONE,
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
        val previousItem = snapshot.itemFor(step)
        val updatedItem = HardeningSetupItem(
            step = step,
            status = status,
            evidenceType = evidenceType,
            updatedAtMillis = currentTimeMillis,
            note = note?.trim()?.takeIf(String::isNotEmpty),
        )
        val signal = PinCompromiseSentinel().evaluate(
            change = HardeningStateChange(
                step = step,
                previousStatus = previousItem.status,
                newStatus = status,
                changedAtMillis = currentTimeMillis,
                evidenceType = evidenceType,
                note = note,
            ),
            activeMaintenanceWindow = snapshot.activeMaintenanceWindow,
            currentTimeMillis = currentTimeMillis,
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
            latestPinCompromiseSignal = if (signal.suspected) signal else snapshot.latestPinCompromiseSignal,
        )
        return updatedSnapshot.copy(summaryStatus = summarize(updatedSnapshot))
    }

    fun openMaintenanceWindow(
        snapshot: HardeningSetupSnapshot,
        window: ParentMaintenanceWindow,
        currentTimeMillis: Long,
    ): HardeningSetupSnapshot {
        val withWindow = snapshot.copy(
            generatedAtMillis = currentTimeMillis,
            activeMaintenanceWindow = window,
        )
        return updateStep(
            snapshot = withWindow,
            step = HardeningSetupStep.PARENT_MAINTENANCE_WINDOW,
            status = HardeningSetupStatus.PARENT_CONFIRMED,
            evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
            note = "Parent maintenance window active for ${window.reason}",
            currentTimeMillis = currentTimeMillis,
        )
    }

    fun closeMaintenanceWindow(
        snapshot: HardeningSetupSnapshot,
        currentTimeMillis: Long,
    ): HardeningSetupSnapshot {
        return snapshot.copy(
            generatedAtMillis = currentTimeMillis,
            activeMaintenanceWindow = null,
            summaryStatus = summarize(snapshot),
        )
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
        val bypassRiskSteps = listOf(
            HardeningSetupStep.DEVELOPER_OPTIONS_DISABLED,
            HardeningSetupStep.USB_DEBUGGING_DISABLED,
            HardeningSetupStep.WIRELESS_DEBUGGING_DISABLED,
            HardeningSetupStep.NO_UNRESTRICTED_SECONDARY_USERS,
            HardeningSetupStep.NO_UNRESTRICTED_WORK_PROFILE,
            HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY,
            HardeningSetupStep.PARENT_PIN_NOT_SHARED,
            HardeningSetupStep.UNKNOWN_SOURCES_REVIEWED,
        )
        if (bypassRiskSteps.any { step -> !items.getValue(step).status.isConfirmed() }) {
            return HardeningSummaryStatus.NEEDS_ATTENTION
        }
        if (snapshot.latestPinCompromiseSignal.suspected) {
            return HardeningSummaryStatus.NEEDS_ATTENTION
        }
        return HardeningSummaryStatus.READY_FOR_LAB_TEST
    }

    private fun HardeningSetupStatus.isConfirmed(): Boolean {
        return this == HardeningSetupStatus.USER_CONFIRMED ||
            this == HardeningSetupStatus.AUTO_CONFIRMED ||
            this == HardeningSetupStatus.CONFIRMED_DISABLED ||
            this == HardeningSetupStatus.CONFIRMED_ABSENT ||
            this == HardeningSetupStatus.BEST_EFFORT_AUTO_CHECK ||
            this == HardeningSetupStatus.PARENT_CONFIRMED
    }
}

class PinCompromiseSentinel {
    fun evaluate(
        change: HardeningStateChange,
        activeMaintenanceWindow: ParentMaintenanceWindow?,
        currentTimeMillis: Long,
    ): PinCompromiseSignal {
        if (!change.isCritical()) {
            return PinCompromiseSignal.NONE
        }
        if (activeMaintenanceWindow?.isActive(currentTimeMillis) == true) {
            return PinCompromiseSignal(
                suspected = false,
                reason = "Critical hardening change happened during parent maintenance window",
                changedStep = change.step,
                changedAtMillis = change.changedAtMillis,
                activeMaintenanceWindowId = activeMaintenanceWindow.windowId,
            )
        }
        return PinCompromiseSignal(
            suspected = true,
            reason = "Parent PIN may be compromised",
            changedStep = change.step,
            changedAtMillis = change.changedAtMillis,
            activeMaintenanceWindowId = null,
        )
    }

    private fun HardeningStateChange.isCritical(): Boolean {
        if (previousStatus == newStatus) {
            return false
        }
        return when (step) {
            HardeningSetupStep.VPN_ALWAYS_ON,
            HardeningSetupStep.BLOCK_WITHOUT_VPN,
            HardeningSetupStep.SETTINGS_APP_LOCK,
            HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY,
            HardeningSetupStep.DEVELOPER_OPTIONS_DISABLED,
            HardeningSetupStep.USB_DEBUGGING_DISABLED,
            HardeningSetupStep.WIRELESS_DEBUGGING_DISABLED,
            HardeningSetupStep.NO_UNRESTRICTED_SECONDARY_USERS,
            HardeningSetupStep.NO_UNRESTRICTED_WORK_PROFILE,
            HardeningSetupStep.VPN_LIFECYCLE_HEALTH,
            -> newStatus == HardeningSetupStatus.NEEDS_ATTENTION ||
                newStatus == HardeningSetupStatus.UNKNOWN ||
                newStatus == HardeningSetupStatus.COMPROMISE_SUSPECTED
            else -> false
        }
    }
}
