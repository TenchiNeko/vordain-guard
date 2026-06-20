package com.vordain.guard.core.devrelay

import com.vordain.guard.core.model.DeviceId
import java.nio.charset.StandardCharsets
import java.util.Base64

class DevRelayDebugCommandCodec {
    fun encodeCommand(command: DevRelayDebugCommand): String {
        return listOf(
            COMMAND_HEADER,
            "commandId=${command.commandId}",
            "type=${command.type.name}",
            "sourceDeviceId=${command.sourceDeviceId.value}",
            "targetDeviceId=${command.targetDeviceId.value}",
            "createdAtMillis=${command.createdAtMillis}",
            "status=${command.status.name}",
            "warning=$WARNING_TEXT",
        ).joinToString(separator = "\n")
    }

    fun decodeCommand(text: String): DevRelayDebugCommandCodecResult {
        val lines = normalizedLines(text)
        if (lines.firstOrNull() != COMMAND_HEADER) {
            return DevRelayDebugCommandCodecResult(false, "Wrong debug command header")
        }
        val values = keyValues(lines.drop(1))
        forbiddenField(values)?.let { field ->
            return DevRelayDebugCommandCodecResult(false, "Forbidden sensitive field: $field")
        }
        val commandId = values["commandId"]?.takeIf(String::isNotBlank)
            ?: return DevRelayDebugCommandCodecResult(false, "Missing commandId")
        val type = values["type"]?.let(::enumCommandType)
            ?: return DevRelayDebugCommandCodecResult(false, "Unknown command type")
        val sourceDeviceId = values["sourceDeviceId"]?.takeIf(String::isNotBlank)
            ?: return DevRelayDebugCommandCodecResult(false, "Missing sourceDeviceId")
        val targetDeviceId = values["targetDeviceId"]?.takeIf(String::isNotBlank)
            ?: return DevRelayDebugCommandCodecResult(false, "Missing targetDeviceId")
        val createdAtMillis = values["createdAtMillis"]?.toLongOrNull()
            ?: return DevRelayDebugCommandCodecResult(false, "Malformed createdAtMillis")
        val status = values["status"]?.let(::enumCommandStatus)
            ?: return DevRelayDebugCommandCodecResult(false, "Unknown command status")
        return DevRelayDebugCommandCodecResult(
            accepted = true,
            reason = "Accepted local debug command",
            command = DevRelayDebugCommand(
                commandId = commandId,
                type = type,
                sourceDeviceId = DeviceId(sourceDeviceId),
                targetDeviceId = DeviceId(targetDeviceId),
                createdAtMillis = createdAtMillis,
                status = status,
            ),
        )
    }

    fun encodeCommands(commands: List<DevRelayDebugCommand>): String {
        val header = listOf(
            COMMANDS_HEADER,
            "commandCount=${commands.size}",
            "warning=$WARNING_TEXT",
        )
        val payloads = commands.flatMapIndexed { index, command ->
            listOf("command.$index.text=${encodeText(encodeCommand(command))}")
        }
        return (header + payloads).joinToString(separator = "\n")
    }

    fun decodeCommands(text: String): DevRelayDebugCommandsCodecResult {
        val lines = normalizedLines(text)
        if (lines.firstOrNull() != COMMANDS_HEADER) {
            return DevRelayDebugCommandsCodecResult(false, "Wrong debug commands header")
        }
        val values = keyValues(lines.drop(1))
        forbiddenField(values)?.let { field ->
            return DevRelayDebugCommandsCodecResult(false, "Forbidden sensitive field: $field")
        }
        val count = values["commandCount"]?.toIntOrNull()
            ?: return DevRelayDebugCommandsCodecResult(false, "Malformed commandCount")
        if (count < 0) {
            return DevRelayDebugCommandsCodecResult(false, "Malformed commandCount")
        }
        val commands = mutableListOf<DevRelayDebugCommand>()
        repeat(count) { index ->
            val encoded = values["command.$index.text"]
                ?: return DevRelayDebugCommandsCodecResult(false, "Missing command text")
            val decoded = decodeCommand(decodeText(encoded))
            if (!decoded.accepted || decoded.command == null) {
                return DevRelayDebugCommandsCodecResult(false, decoded.reason)
            }
            commands += decoded.command
        }
        return DevRelayDebugCommandsCodecResult(true, "Accepted local debug commands", commands)
    }

    fun encodeResult(result: DevRelayDebugCommandResult): String {
        return listOf(
            RESULT_HEADER,
            "resultId=${result.resultId}",
            "commandId=${result.commandId}",
            "type=${result.type.name}",
            "sourceDeviceId=${result.sourceDeviceId.value}",
            "targetDeviceId=${result.targetDeviceId.value}",
            "createdAtMillis=${result.createdAtMillis}",
            "success=${result.success}",
            "summary=${encodeText(result.summary)}",
            "warning=$WARNING_TEXT",
        ).joinToString(separator = "\n")
    }

    fun decodeResult(text: String): DevRelayDebugCommandResultCodecResult {
        val lines = normalizedLines(text)
        if (lines.firstOrNull() != RESULT_HEADER) {
            return DevRelayDebugCommandResultCodecResult(false, "Wrong debug command result header")
        }
        val values = keyValues(lines.drop(1))
        forbiddenField(values)?.let { field ->
            return DevRelayDebugCommandResultCodecResult(false, "Forbidden sensitive field: $field")
        }
        val resultId = values["resultId"]?.takeIf(String::isNotBlank)
            ?: return DevRelayDebugCommandResultCodecResult(false, "Missing resultId")
        val commandId = values["commandId"]?.takeIf(String::isNotBlank)
            ?: return DevRelayDebugCommandResultCodecResult(false, "Missing commandId")
        val type = values["type"]?.let(::enumCommandType)
            ?: return DevRelayDebugCommandResultCodecResult(false, "Unknown command type")
        val sourceDeviceId = values["sourceDeviceId"]?.takeIf(String::isNotBlank)
            ?: return DevRelayDebugCommandResultCodecResult(false, "Missing sourceDeviceId")
        val targetDeviceId = values["targetDeviceId"]?.takeIf(String::isNotBlank)
            ?: return DevRelayDebugCommandResultCodecResult(false, "Missing targetDeviceId")
        val createdAtMillis = values["createdAtMillis"]?.toLongOrNull()
            ?: return DevRelayDebugCommandResultCodecResult(false, "Malformed createdAtMillis")
        val success = values["success"]?.toBooleanStrictOrNull()
            ?: return DevRelayDebugCommandResultCodecResult(false, "Malformed success")
        val summary = values["summary"]?.let(::decodeText)
            ?: return DevRelayDebugCommandResultCodecResult(false, "Missing summary")
        return DevRelayDebugCommandResultCodecResult(
            accepted = true,
            reason = "Accepted local debug command result",
            result = DevRelayDebugCommandResult(
                resultId = resultId,
                commandId = commandId,
                type = type,
                sourceDeviceId = DeviceId(sourceDeviceId),
                targetDeviceId = DeviceId(targetDeviceId),
                createdAtMillis = createdAtMillis,
                success = success,
                summary = summary,
            ),
        )
    }

    fun encodeResults(results: List<DevRelayDebugCommandResult>): String {
        val header = listOf(
            RESULTS_HEADER,
            "resultCount=${results.size}",
            "warning=$WARNING_TEXT",
        )
        val payloads = results.flatMapIndexed { index, result ->
            listOf("result.$index.text=${encodeText(encodeResult(result))}")
        }
        return (header + payloads).joinToString(separator = "\n")
    }

    fun decodeResults(text: String): DevRelayDebugCommandResultsCodecResult {
        val lines = normalizedLines(text)
        if (lines.firstOrNull() != RESULTS_HEADER) {
            return DevRelayDebugCommandResultsCodecResult(false, "Wrong debug command results header")
        }
        val values = keyValues(lines.drop(1))
        forbiddenField(values)?.let { field ->
            return DevRelayDebugCommandResultsCodecResult(false, "Forbidden sensitive field: $field")
        }
        val count = values["resultCount"]?.toIntOrNull()
            ?: return DevRelayDebugCommandResultsCodecResult(false, "Malformed resultCount")
        if (count < 0) {
            return DevRelayDebugCommandResultsCodecResult(false, "Malformed resultCount")
        }
        val results = mutableListOf<DevRelayDebugCommandResult>()
        repeat(count) { index ->
            val encoded = values["result.$index.text"]
                ?: return DevRelayDebugCommandResultsCodecResult(false, "Missing result text")
            val decoded = decodeResult(decodeText(encoded))
            if (!decoded.accepted || decoded.result == null) {
                return DevRelayDebugCommandResultsCodecResult(false, decoded.reason)
            }
            results += decoded.result
        }
        return DevRelayDebugCommandResultsCodecResult(true, "Accepted local debug command results", results)
    }

    private fun normalizedLines(text: String): List<String> {
        return text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
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

    private fun forbiddenField(values: Map<String, String>): String? {
        return values.keys.firstOrNull { key -> key in forbiddenFieldNames }
    }

    private fun enumCommandType(value: String): DevRelayDebugCommandType? {
        return enumValues<DevRelayDebugCommandType>().firstOrNull { it.name == value }
    }

    private fun enumCommandStatus(value: String): DevRelayDebugCommandStatus? {
        return enumValues<DevRelayDebugCommandStatus>().firstOrNull { it.name == value }
    }

    private fun encodeText(value: String): String {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decodeText(value: String): String {
        return String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    }

    companion object {
        const val COMMAND_HEADER = "VORDAIN_DEBUG_REMOTE_TEST_COMMAND_V1"
        const val COMMANDS_HEADER = "VORDAIN_DEBUG_REMOTE_TEST_COMMANDS_V1"
        const val RESULT_HEADER = "VORDAIN_DEBUG_REMOTE_TEST_RESULT_V1"
        const val RESULTS_HEADER = "VORDAIN_DEBUG_REMOTE_TEST_RESULTS_V1"
        const val WARNING_TEXT = "Local debug remote test harness only. Production builds use no active controller."
        private val forbiddenFieldNames = setOf(
            "pin" + "Value",
            "pin" + "Digits",
            "pass" + "word",
            "creden" + "tial",
            "key" + "stroke",
        )
    }
}
