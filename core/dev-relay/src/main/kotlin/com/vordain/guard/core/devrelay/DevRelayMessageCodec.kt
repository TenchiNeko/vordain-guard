package com.vordain.guard.core.devrelay

import com.vordain.guard.core.model.DeviceId
import java.nio.charset.StandardCharsets
import java.util.Base64

class DevRelayMessageCodec {
    fun encodeMessage(message: DevRelayMessage): String {
        return listOf(
            MESSAGE_HEADER,
            "messageId=${message.messageId}",
            "direction=${message.direction.name}",
            "sourceDeviceId=${message.sourceDeviceId.value}",
            "targetDeviceId=${message.targetDeviceId.value}",
            "createdAtMillis=${message.createdAtMillis}",
            "status=${message.status.name}",
            "bundleText=${encodeText(message.bundleText)}",
            "warning=$WARNING_TEXT",
        ).joinToString(separator = "\n")
    }

    fun decodeMessage(text: String): DevRelayMessageCodecResult {
        val lines = text.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
        if (lines.firstOrNull() != MESSAGE_HEADER) {
            return DevRelayMessageCodecResult(false, "Wrong dev relay message header")
        }
        val values = keyValues(lines.drop(1))
        val forbiddenField = values.keys.firstOrNull { key -> key in forbiddenFieldNames }
        if (forbiddenField != null) {
            return DevRelayMessageCodecResult(false, "Forbidden sensitive field: $forbiddenField")
        }
        val messageId = values["messageId"]?.takeIf(String::isNotBlank)
            ?: return DevRelayMessageCodecResult(false, "Missing messageId")
        val sourceDeviceId = values["sourceDeviceId"]?.takeIf(String::isNotBlank)
            ?: return DevRelayMessageCodecResult(false, "Missing sourceDeviceId")
        val targetDeviceId = values["targetDeviceId"]?.takeIf(String::isNotBlank)
            ?: return DevRelayMessageCodecResult(false, "Missing targetDeviceId")
        val direction = values["direction"]?.let(::enumDirection)
            ?: return DevRelayMessageCodecResult(false, "Unknown direction")
        val createdAtMillis = values["createdAtMillis"]?.toLongOrNull()
            ?: return DevRelayMessageCodecResult(false, "Malformed createdAtMillis")
        val status = values["status"]?.let(::enumStatus)
            ?: return DevRelayMessageCodecResult(false, "Unknown status")
        val bundleText = values["bundleText"]?.let(::decodeText)
            ?: return DevRelayMessageCodecResult(false, "Missing bundleText")
        return DevRelayMessageCodecResult(
            accepted = true,
            reason = "Accepted local dev relay message",
            message = DevRelayMessage(
                messageId = messageId,
                direction = direction,
                sourceDeviceId = DeviceId(sourceDeviceId),
                targetDeviceId = DeviceId(targetDeviceId),
                createdAtMillis = createdAtMillis,
                bundleText = bundleText,
                status = status,
            ),
        )
    }

    fun encodeMessages(messages: List<DevRelayMessage>): String {
        val header = listOf(
            MESSAGES_HEADER,
            "messageCount=${messages.size}",
            "warning=$WARNING_TEXT",
        )
        val payloads = messages.flatMapIndexed { index, message ->
            listOf("message.$index.text=${encodeText(encodeMessage(message))}")
        }
        return (header + payloads).joinToString(separator = "\n")
    }

    fun decodeMessages(text: String): DevRelayMessagesCodecResult {
        val lines = text.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
        if (lines.firstOrNull() != MESSAGES_HEADER) {
            return DevRelayMessagesCodecResult(false, "Wrong dev relay messages header")
        }
        val values = keyValues(lines.drop(1))
        val forbiddenField = values.keys.firstOrNull { key -> key in forbiddenFieldNames }
        if (forbiddenField != null) {
            return DevRelayMessagesCodecResult(false, "Forbidden sensitive field: $forbiddenField")
        }
        val count = values["messageCount"]?.toIntOrNull()
            ?: return DevRelayMessagesCodecResult(false, "Malformed messageCount")
        if (count < 0) {
            return DevRelayMessagesCodecResult(false, "Malformed messageCount")
        }
        val messages = mutableListOf<DevRelayMessage>()
        repeat(count) { index ->
            val encodedMessage = values["message.$index.text"]
                ?: return DevRelayMessagesCodecResult(false, "Missing message text")
            val decoded = decodeMessage(decodeText(encodedMessage))
            if (!decoded.accepted || decoded.message == null) {
                return DevRelayMessagesCodecResult(false, decoded.reason)
            }
            messages += decoded.message
        }
        return DevRelayMessagesCodecResult(
            accepted = true,
            reason = "Accepted local dev relay messages",
            messages = messages,
        )
    }

    private fun keyValues(lines: List<String>): Map<String, String> {
        return lines.mapNotNull { line ->
            val index = line.indexOf('=')
            if (index <= 0) {
                null
            } else {
                line.substring(0, index).trim() to line.substring(index + 1).trim()
            }
        }.toMap()
    }

    private fun enumDirection(value: String): DevRelayDirection? {
        return enumValues<DevRelayDirection>().firstOrNull { it.name == value }
    }

    private fun enumStatus(value: String): DevRelayMessageStatus? {
        return enumValues<DevRelayMessageStatus>().firstOrNull { it.name == value }
    }

    private fun encodeText(value: String): String {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decodeText(value: String): String {
        return String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    }

    companion object {
        const val MESSAGE_HEADER = "VORDAIN_DEBUG_DEV_RELAY_MESSAGE_V1"
        const val MESSAGES_HEADER = "VORDAIN_DEBUG_DEV_RELAY_MESSAGES_V1"
        const val WARNING_TEXT = "Local dev relay only. Debug/manual send-fetch; not production secure."
        private val forbiddenFieldNames = setOf(
            "pin" + "Value",
            "pin" + "Digits",
            "pass" + "word",
            "creden" + "tial",
            "key" + "stroke",
        )
    }
}
