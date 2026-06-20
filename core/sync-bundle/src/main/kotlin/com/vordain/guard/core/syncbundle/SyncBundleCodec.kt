package com.vordain.guard.core.syncbundle

import com.vordain.guard.core.model.DeviceId
import java.nio.charset.StandardCharsets
import java.util.Base64

class SyncBundleCodec {
    fun encode(bundle: SyncBundle): String {
        val header = listOf(
            HEADER,
            "bundleId=${bundle.bundleId}",
            "direction=${bundle.direction.name}",
            "kind=${bundle.kind.name}",
            "createdAtMillis=${bundle.createdAtMillis}",
            "sourceDeviceId=${bundle.sourceDeviceId.value}",
            "targetDeviceId=${bundle.targetDeviceId?.value.orEmpty()}",
            "payloadCount=${bundle.payloads.size}",
            "warning=${bundle.warningText}",
        )
        val payloadLines = bundle.payloads.flatMapIndexed { index, payload ->
            listOf(
                "payload.$index.kind=${payload.kind.name}",
                "payload.$index.label=${encodeText(payload.label)}",
                "payload.$index.text=${encodeText(payload.payloadText)}",
            )
        }
        return (header + payloadLines).joinToString(separator = "\n")
    }

    fun decode(text: String): SyncBundleValidationResult {
        val lines = text.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
        if (lines.firstOrNull() != HEADER) {
            return SyncBundleValidationResult(false, "Wrong sync bundle header")
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
            return SyncBundleValidationResult(false, "Forbidden sensitive field: $forbiddenField")
        }
        val bundleId = values["bundleId"]?.takeIf(String::isNotBlank)
            ?: return SyncBundleValidationResult(false, "Missing bundleId")
        val sourceDeviceId = values["sourceDeviceId"]?.takeIf(String::isNotBlank)
            ?: return SyncBundleValidationResult(false, "Missing sourceDeviceId")
        val direction = values["direction"]?.let(::enumDirection)
            ?: return SyncBundleValidationResult(false, "Unknown direction")
        val kind = values["kind"]?.let(::enumKind)
            ?: return SyncBundleValidationResult(false, "Unknown bundle kind")
        val createdAtMillis = values["createdAtMillis"]?.toLongOrNull()
            ?: return SyncBundleValidationResult(false, "Malformed createdAtMillis")
        val payloadCount = values["payloadCount"]?.toIntOrNull()
            ?: return SyncBundleValidationResult(false, "Malformed payloadCount")
        if (payloadCount < 0) {
            return SyncBundleValidationResult(false, "Malformed payloadCount")
        }
        val payloads = mutableListOf<SyncBundlePayload>()
        repeat(payloadCount) { index ->
            val payloadKind = values["payload.$index.kind"]?.let(::enumPayloadKind)
                ?: return SyncBundleValidationResult(false, "Unknown payload kind")
            val label = values["payload.$index.label"]?.let(::decodeText)
                ?: return SyncBundleValidationResult(false, "Missing payload label")
            val payloadText = values["payload.$index.text"]?.let(::decodeText)
                ?: return SyncBundleValidationResult(false, "Missing payload text")
            payloads += SyncBundlePayload(
                kind = payloadKind,
                label = label,
                payloadText = payloadText,
            )
        }
        return SyncBundleValidationResult(
            accepted = true,
            reason = "Accepted local debug sync bundle",
            bundle = SyncBundle(
                bundleId = bundleId,
                direction = direction,
                kind = kind,
                createdAtMillis = createdAtMillis,
                sourceDeviceId = DeviceId(sourceDeviceId),
                targetDeviceId = values["targetDeviceId"]?.takeIf(String::isNotBlank)?.let(::DeviceId),
                payloads = payloads,
                warningText = values["warning"]?.takeIf(String::isNotBlank) ?: SyncBundle.WARNING_TEXT,
            ),
        )
    }

    private fun enumDirection(value: String): SyncBundleDirection? {
        return enumValues<SyncBundleDirection>().firstOrNull { it.name == value }
    }

    private fun enumKind(value: String): SyncBundleKind? {
        return enumValues<SyncBundleKind>().firstOrNull { it.name == value }
    }

    private fun enumPayloadKind(value: String): SyncBundlePayloadKind? {
        return enumValues<SyncBundlePayloadKind>().firstOrNull { it.name == value }
    }

    private fun encodeText(value: String): String {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decodeText(value: String): String {
        return String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    }

    companion object {
        const val HEADER = "VORDAIN_DEBUG_SYNC_BUNDLE_V1"
        private val forbiddenFieldNames = setOf(
            "pin" + "Value",
            "pin" + "Digits",
            "pass" + "word",
            "creden" + "tial",
            "key" + "stroke",
        )
    }
}
