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

        val items = mutableListOf<HardeningSetupItem>()
        for (step in HardeningSetupStep.entries) {
            val encodedItem = values[step.name] ?: continue
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

        val knownKeys = setOf("childDeviceId", "generatedAtMillis") + HardeningSetupStep.entries.map { step -> step.name }
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
        )
        return DebugHardeningSetupReportCodecResult.Decoded(
            snapshot.copy(summaryStatus = reducer.summarize(snapshot)),
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? {
        return enumValues<T>().firstOrNull { enumValue -> enumValue.name == value }
    }

    companion object {
        const val HEADER = "VORDAIN_DEBUG_HARDENING_REPORT_V1"
    }
}
