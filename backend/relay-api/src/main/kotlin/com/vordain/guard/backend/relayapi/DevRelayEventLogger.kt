package com.vordain.guard.backend.relayapi

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class DevRelayEventLogger(
    private val logPath: Path = Path.of("backend/relay-api/.dev-relay-events.ndjson"),
) {
    @Synchronized
    fun log(event: DevRelayEvent) {
        logPath.parent?.let(Files::createDirectories)
        Files.writeString(
            logPath,
            event.toNdjsonLine() + "\n",
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
        )
    }
}

data class DevRelayEvent(
    val occurredAtMillis: Long,
    val method: String,
    val path: String,
    val remoteAddress: String,
    val accepted: Boolean,
    val result: String,
    val messageId: String? = null,
    val commandId: String? = null,
    val resultId: String? = null,
    val sourceDeviceId: String? = null,
    val targetDeviceId: String? = null,
) {
    fun toNdjsonLine(): String {
        return listOfNotNull(
            "occurredAtMillis" to occurredAtMillis.toString(),
            "method" to method,
            "path" to path,
            "remoteAddress" to remoteAddress,
            "accepted" to accepted.toString(),
            "result" to result,
            messageId?.let { "messageId" to it },
            commandId?.let { "commandId" to it },
            resultId?.let { "resultId" to it },
            sourceDeviceId?.let { "sourceDeviceId" to it },
            targetDeviceId?.let { "targetDeviceId" to it },
        ).joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "\"${escape(key)}\":\"${escape(value)}\""
        }
    }

    private fun escape(value: String): String {
        return buildString {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }
}
