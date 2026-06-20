package com.vordain.guard.child

import com.vordain.guard.core.devrelay.DevRelayMessage
import com.vordain.guard.core.devrelay.DevRelayMessageCodec
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ChildDevRelayClientResult(
    val success: Boolean,
    val summary: String,
    val messages: List<DevRelayMessage> = emptyList(),
)

class ChildLocalDevRelayClient(
    private val codec: DevRelayMessageCodec = DevRelayMessageCodec(),
) {
    fun sendMessage(
        baseUrl: String,
        message: DevRelayMessage,
    ): ChildDevRelayClientResult {
        val response = request(
            method = "POST",
            url = "${baseUrl.trimEnd('/')}/debug/v1/messages",
            body = codec.encodeMessage(message),
        )
        return ChildDevRelayClientResult(
            success = response.code in 200..299,
            summary = "send ${response.code}: ${response.body}",
        )
    }

    fun fetchMessages(
        baseUrl: String,
        targetDeviceId: String,
    ): ChildDevRelayClientResult {
        val response = request(
            method = "GET",
            url = "${baseUrl.trimEnd('/')}/debug/v1/messages?targetDeviceId=${targetDeviceId.encodeQuery()}",
            body = null,
        )
        if (response.code !in 200..299) {
            return ChildDevRelayClientResult(false, "fetch ${response.code}: ${response.body}")
        }
        val decoded = codec.decodeMessages(response.body)
        return ChildDevRelayClientResult(
            success = decoded.accepted,
            summary = if (decoded.accepted) {
                "fetch accepted: ${decoded.messages.size} message(s)"
            } else {
                "fetch rejected: ${decoded.reason}"
            },
            messages = decoded.messages,
        )
    }

    fun ackMessage(
        baseUrl: String,
        messageId: String,
    ): ChildDevRelayClientResult {
        val response = request(
            method = "POST",
            url = "${baseUrl.trimEnd('/')}/debug/v1/ack?messageId=${messageId.encodeQuery()}",
            body = "",
        )
        return ChildDevRelayClientResult(
            success = response.code in 200..299,
            summary = "ack ${response.code}: ${response.body}",
        )
    }

    private fun request(
        method: String,
        url: String,
        body: String?,
    ): RelayHttpResponse {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        if (body != null) {
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
            connection.outputStream.use { output ->
                output.write(bytes)
            }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream?.use { input ->
            input.readBytes().toString(StandardCharsets.UTF_8)
        }.orEmpty()
        connection.disconnect()
        return RelayHttpResponse(code, responseBody)
    }

    private fun String.encodeQuery(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8.name())
    }

    private data class RelayHttpResponse(
        val code: Int,
        val body: String,
    )

    companion object {
        private const val TIMEOUT_MILLIS = 2_000
    }
}
