package com.vordain.guard.features.setupchecklist

import com.vordain.guard.core.model.DeviceId

sealed class DebugHardeningSetupReportCodecResult {
    data class Decoded(val snapshot: HardeningSetupSnapshot) : DebugHardeningSetupReportCodecResult()
    data class Rejected(val reason: String) : DebugHardeningSetupReportCodecResult()
}

class DebugHardeningSetupReportCodec {
    fun encode(snapshot: HardeningSetupSnapshot): String {
        val lines = mutableListOf(
            HEADER,
            "childDeviceId=${snapshot.childDeviceId.value}",
            "generatedAtMillis=${snapshot.generatedAtMillis}",
        )
        snapshot.activeMaintenanceWindow?.let { window ->
            lines += "maintenanceWindow=${window.windowId}|${window.openedAtMillis}|${window.expiresAtMillis}|" +
                "${window.reason.name}|${window.openedByParentDeviceId?.value.orEmpty()}"
        }
        snapshot.latestPinCompromiseSignal.let { signal ->
            lines += "pinCompromiseSignal=${signal.suspected}|${signal.reason}|" +
                "${signal.changedStep?.name.orEmpty()}|${signal.changedAtMillis ?: ""}|" +
                signal.activeMaintenanceWindowId.orEmpty()
        }
        HardeningSetupStep.entries.forEach { step ->
            val item = snapshot.itemFor(step)
            lines += "${step.name}=${item.status.name}|${item.evidenceType.name}|${item.note.orEmpty()}"
        }
        return lines.joinToString(separator = "\n")
    }

    fun decode(payload: String): DebugHardeningSetupReportCodecResult {
        val lines = payload.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
        if (lines.firstOrNull() != HEADER) {
            return DebugHardeningSetupReportCodecResult.Rejected("Wrong debug hardening report header")
        }
        val values = lines.drop(1).mapNotNull { line ->
            val index = line.indexOf('=')
            if (index <= 0) {
                null
            } else {
                line.substring(0, index).trim() to line.substring(index + 1).trim()
            }
        }.toMap()
        val childDeviceId = values["childDeviceId"]?.takeIf(String::isNotBlank)
            ?: return DebugHardeningSetupReportCodecResult.Rejected("Missing childDeviceId")
        val generatedAtMillis = values["generatedAtMillis"]?.toLongOrNull()
            ?: return DebugHardeningSetupReportCodecResult.Rejected("Malformed generatedAtMillis")
        val forbiddenField = values.keys.firstOrNull { key -> key in forbiddenFieldNames }
        if (forbiddenField != null) {
            return DebugHardeningSetupReportCodecResult.Rejected("Forbidden sensitive field: $forbiddenField")
        }

        val items = mutableListOf<HardeningSetupItem>()
        for (step in HardeningSetupStep.entries) {
            val encodedItem = values[step.name] ?: if (step == HardeningSetupStep.UNKNOWN_SOURCES_REVIEWED) {
                values[LEGACY_UNKNOWN_SOURCES_REVIEW]
            } else {
                null
            } ?: continue
            val parts = encodedItem.split('|', limit = 3)
            if (parts.size < 2) {
                return DebugHardeningSetupReportCodecResult.Rejected("Malformed item for ${step.name}")
            }
            val status = enumValueOrNull<HardeningSetupStatus>(parts[0].trim())
                ?: return DebugHardeningSetupReportCodecResult.Rejected("Unknown status for ${step.name}")
            val evidenceType = enumValueOrNull<HardeningEvidenceType>(parts[1].trim())
                ?: return DebugHardeningSetupReportCodecResult.Rejected("Unknown evidence type for ${step.name}")
            items += HardeningSetupItem(
                step = step,
                status = status,
                evidenceType = evidenceType,
                updatedAtMillis = generatedAtMillis,
                note = parts.getOrNull(2)?.trim()?.takeIf(String::isNotEmpty),
            )
        }

        val activeMaintenanceWindow = values["maintenanceWindow"]?.let { encoded ->
            decodeMaintenanceWindow(encoded)
                ?: return DebugHardeningSetupReportCodecResult.Rejected("Malformed maintenanceWindow")
        }
        val compromiseSignal = values["pinCompromiseSignal"]?.let { encoded ->
            decodeCompromiseSignal(encoded)
                ?: return DebugHardeningSetupReportCodecResult.Rejected("Malformed pinCompromiseSignal")
        } ?: PinCompromiseSignal.NONE

        val knownKeys = setOf(
            "childDeviceId",
            "generatedAtMillis",
            "maintenanceWindow",
            "pinCompromiseSignal",
            LEGACY_UNKNOWN_SOURCES_REVIEW,
        ) + HardeningSetupStep.entries.map { step -> step.name }
        val unknownStepKey = values.keys.firstOrNull { key ->
            key !in knownKeys && key.uppercase() == key && key.contains('_')
        }
        if (unknownStepKey != null) {
            return DebugHardeningSetupReportCodecResult.Rejected("Unknown setup step: $unknownStepKey")
        }

        val reducer = HardeningSetupReducer()
        val withDefaults = HardeningSetupStep.entries.map { step ->
            items.firstOrNull { item -> item.step == step }
                ?: HardeningSetupItem(
                    step = step,
                    status = HardeningSetupStatus.UNKNOWN,
                    evidenceType = HardeningEvidenceType.UNKNOWN,
                    updatedAtMillis = generatedAtMillis,
                )
        }
        val snapshot = HardeningSetupSnapshot(
            childDeviceId = DeviceId(childDeviceId),
            generatedAtMillis = generatedAtMillis,
            items = withDefaults,
            summaryStatus = HardeningSummaryStatus.UNKNOWN,
            activeMaintenanceWindow = activeMaintenanceWindow,
            latestPinCompromiseSignal = compromiseSignal,
        )
        return DebugHardeningSetupReportCodecResult.Decoded(
            snapshot.copy(summaryStatus = reducer.summarize(snapshot)),
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? {
        return enumValues<T>().firstOrNull { enumValue -> enumValue.name == value }
    }

    private fun decodeMaintenanceWindow(encoded: String): ParentMaintenanceWindow? {
        val parts = encoded.split('|', limit = 5)
        if (parts.size < 4) {
            return null
        }
        val reason = enumValueOrNull<MaintenanceWindowReason>(parts[3].trim()) ?: return null
        return runCatching {
            ParentMaintenanceWindow(
                windowId = parts[0].trim(),
                openedAtMillis = parts[1].trim().toLong(),
                expiresAtMillis = parts[2].trim().toLong(),
                reason = reason,
                openedByParentDeviceId = parts.getOrNull(4)?.trim()?.takeIf(String::isNotBlank)?.let(::DeviceId),
            )
        }.getOrNull()
    }

    private fun decodeCompromiseSignal(encoded: String): PinCompromiseSignal? {
        val parts = encoded.split('|', limit = 5)
        if (parts.size < 2) {
            return null
        }
        val step = parts.getOrNull(2)?.trim()?.takeIf(String::isNotBlank)?.let { encodedStep ->
            enumValueOrNull<HardeningSetupStep>(encodedStep) ?: return null
        }
        val changedAtMillis = parts.getOrNull(3)?.trim()?.takeIf(String::isNotBlank)?.toLongOrNull()
        return PinCompromiseSignal(
            suspected = parts[0].trim().toBooleanStrictOrNull() ?: return null,
            reason = parts[1].trim(),
            changedStep = step,
            changedAtMillis = changedAtMillis,
            activeMaintenanceWindowId = parts.getOrNull(4)?.trim()?.takeIf(String::isNotBlank),
        )
    }

    companion object {
        const val HEADER = "VORDAIN_DEBUG_HARDENING_REPORT_V1"
        private const val LEGACY_UNKNOWN_SOURCES_REVIEW = "UNKNOWN_SOURCES_REVIEW"
        private val forbiddenFieldNames = setOf(
            "pin" + "Value",
            "pin" + "Digits",
            "password",
            "key" + "strokes",
            "credential",
        )
    }
}
