package com.vordain.guard.backend.relayapi

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.vordain.guard.core.devrelay.DevRelayDebugCommandCodec
import com.vordain.guard.core.devrelay.DevRelayInboxQuery
import com.vordain.guard.core.devrelay.DevRelayMessageCodec
import com.vordain.guard.core.model.DeviceId
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class DevRelayHttpServer(
    private val bindHost: String,
    private val port: Int,
    private val store: InMemoryDevRelayStore = InMemoryDevRelayStore(),
    private val codec: DevRelayMessageCodec = DevRelayMessageCodec(),
    private val debugCommandCodec: DevRelayDebugCommandCodec = DevRelayDebugCommandCodec(),
    private val eventLogger: DevRelayEventLogger = DevRelayEventLogger(),
) {
    private var server: HttpServer? = null

    fun start(): Int {
        val httpServer = HttpServer.create(InetSocketAddress(bindHost, port), 0)
        httpServer.executor = Executors.newCachedThreadPool()
        httpServer.createContext("/health") { exchange ->
            if (exchange.requestMethod != "GET") {
                log(exchange, accepted = false, result = "method_not_allowed")
                exchange.respond(405, "Method not allowed")
            } else {
                log(exchange, accepted = true, result = "ok")
                exchange.respond(200, "OK")
            }
        }
        httpServer.createContext("/debug/v1/messages") { exchange ->
            when (exchange.requestMethod) {
                "POST" -> postMessage(exchange)
                "GET" -> getMessages(exchange)
                else -> {
                    log(exchange, accepted = false, result = "method_not_allowed")
                    exchange.respond(405, "Method not allowed")
                }
            }
        }
        httpServer.createContext("/debug/v1/ack") { exchange ->
            if (exchange.requestMethod == "POST") {
                ackMessage(exchange)
            } else {
                log(exchange, accepted = false, result = "method_not_allowed")
                exchange.respond(405, "Method not allowed")
            }
        }
        httpServer.createContext("/debug/v1/stats") { exchange ->
            if (exchange.requestMethod == "GET") {
                val stats = store.stats()
                exchange.respond(
                    200,
                    listOf(
                        "total=${stats.totalCount}",
                        "pending=${stats.pendingCount}",
                        "fetched=${stats.fetchedCount}",
                        "acknowledged=${stats.acknowledgedCount}",
                        "pendingDebugCommands=${stats.pendingDebugCommandCount}",
                        "debugResults=${stats.debugResultCount}",
                    ).joinToString(separator = "\n"),
                )
                log(exchange, accepted = true, result = "stats")
            } else {
                log(exchange, accepted = false, result = "method_not_allowed")
                exchange.respond(405, "Method not allowed")
            }
        }
        httpServer.createContext("/debug/v1/test-commands") { exchange ->
            when (exchange.requestMethod) {
                "POST" -> postDebugCommand(exchange)
                "GET" -> getDebugCommands(exchange)
                else -> {
                    log(exchange, accepted = false, result = "method_not_allowed")
                    exchange.respond(405, "Method not allowed")
                }
            }
        }
        httpServer.createContext("/debug/v1/test-results") { exchange ->
            when (exchange.requestMethod) {
                "POST" -> postDebugResult(exchange)
                "GET" -> getDebugResults(exchange)
                else -> {
                    log(exchange, accepted = false, result = "method_not_allowed")
                    exchange.respond(405, "Method not allowed")
                }
            }
        }
        httpServer.start()
        server = httpServer
        return httpServer.address.port
    }

    fun stop(delaySeconds: Int = 0) {
        server?.stop(delaySeconds)
        server = null
    }

    private fun postMessage(exchange: HttpExchange) {
        val body = exchange.requestBody.use { input ->
            input.readBytes().toString(StandardCharsets.UTF_8)
        }
        val decoded = codec.decodeMessage(body)
        val message = decoded.message
        if (!decoded.accepted || message == null) {
            log(exchange, accepted = false, result = decoded.reason)
            exchange.respond(400, "rejected=${decoded.reason}")
            return
        }
        store.put(message)
        log(
            exchange = exchange,
            accepted = true,
            result = "message_accepted",
            messageId = message.messageId,
            sourceDeviceId = message.sourceDeviceId.value,
            targetDeviceId = message.targetDeviceId.value,
        )
        exchange.respond(202, "accepted=${message.messageId}")
    }

    private fun getMessages(exchange: HttpExchange) {
        val params = queryParams(exchange.requestURI.rawQuery.orEmpty())
        val targetDeviceId = params["targetDeviceId"]?.takeIf(String::isNotBlank)
        if (targetDeviceId == null) {
            log(exchange, accepted = false, result = "missing_target_device_id")
            exchange.respond(400, "Missing targetDeviceId")
            return
        }
        val messages = store.query(DevRelayInboxQuery(targetDeviceId = DeviceId(targetDeviceId)))
        log(
            exchange = exchange,
            accepted = true,
            result = "messages=${messages.size}",
            targetDeviceId = targetDeviceId,
        )
        exchange.respond(200, codec.encodeMessages(messages))
    }

    private fun ackMessage(exchange: HttpExchange) {
        val params = queryParams(exchange.requestURI.rawQuery.orEmpty())
        val messageId = params["messageId"]?.takeIf(String::isNotBlank)
        if (messageId == null) {
            log(exchange, accepted = false, result = "missing_message_id")
            exchange.respond(400, "Missing messageId")
            return
        }
        val acknowledged = store.acknowledge(messageId)
        if (acknowledged) {
            log(exchange, accepted = true, result = "acknowledged", messageId = messageId)
            exchange.respond(200, "acknowledged=$messageId")
        } else {
            log(exchange, accepted = false, result = "not_found", messageId = messageId)
            exchange.respond(404, "notFound=$messageId")
        }
    }

    private fun postDebugCommand(exchange: HttpExchange) {
        val body = exchange.requestBody.use { input ->
            input.readBytes().toString(StandardCharsets.UTF_8)
        }
        val decoded = debugCommandCodec.decodeCommand(body)
        val command = decoded.command
        if (!decoded.accepted || command == null) {
            log(exchange, accepted = false, result = decoded.reason)
            exchange.respond(400, "rejected=${decoded.reason}")
            return
        }
        store.putDebugCommand(command)
        log(
            exchange = exchange,
            accepted = true,
            result = "command_accepted",
            commandId = command.commandId,
            sourceDeviceId = command.sourceDeviceId.value,
            targetDeviceId = command.targetDeviceId.value,
        )
        exchange.respond(202, "accepted=${command.commandId}")
    }

    private fun getDebugCommands(exchange: HttpExchange) {
        val params = queryParams(exchange.requestURI.rawQuery.orEmpty())
        val targetDeviceId = params["targetDeviceId"]?.takeIf(String::isNotBlank)
        if (targetDeviceId == null) {
            log(exchange, accepted = false, result = "missing_target_device_id")
            exchange.respond(400, "Missing targetDeviceId")
            return
        }
        val commands = store.queryDebugCommands(DeviceId(targetDeviceId))
        log(
            exchange = exchange,
            accepted = true,
            result = "commands=${commands.size}",
            targetDeviceId = targetDeviceId,
        )
        exchange.respond(200, debugCommandCodec.encodeCommands(commands))
    }

    private fun postDebugResult(exchange: HttpExchange) {
        val body = exchange.requestBody.use { input ->
            input.readBytes().toString(StandardCharsets.UTF_8)
        }
        val decoded = debugCommandCodec.decodeResult(body)
        val result = decoded.result
        if (!decoded.accepted || result == null) {
            log(exchange, accepted = false, result = decoded.reason)
            exchange.respond(400, "rejected=${decoded.reason}")
            return
        }
        store.putDebugResult(result)
        log(
            exchange = exchange,
            accepted = true,
            result = "result_accepted",
            commandId = result.commandId,
            resultId = result.resultId,
            sourceDeviceId = result.sourceDeviceId.value,
            targetDeviceId = result.targetDeviceId.value,
        )
        exchange.respond(202, "accepted=${result.resultId}")
    }

    private fun getDebugResults(exchange: HttpExchange) {
        val params = queryParams(exchange.requestURI.rawQuery.orEmpty())
        val targetDeviceId = params["targetDeviceId"]?.takeIf(String::isNotBlank)
        if (targetDeviceId == null) {
            log(exchange, accepted = false, result = "missing_target_device_id")
            exchange.respond(400, "Missing targetDeviceId")
            return
        }
        val results = store.queryDebugResults(DeviceId(targetDeviceId))
        log(
            exchange = exchange,
            accepted = true,
            result = "results=${results.size}",
            targetDeviceId = targetDeviceId,
        )
        exchange.respond(200, debugCommandCodec.encodeResults(results))
    }

    private fun queryParams(rawQuery: String): Map<String, String> {
        if (rawQuery.isBlank()) {
            return emptyMap()
        }
        return rawQuery.split('&').mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) {
                null
            } else {
                decode(part.substring(0, index)) to decode(part.substring(index + 1))
            }
        }.toMap()
    }

    private fun decode(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8)
    }

    private fun log(
        exchange: HttpExchange,
        accepted: Boolean,
        result: String,
        messageId: String? = null,
        commandId: String? = null,
        resultId: String? = null,
        sourceDeviceId: String? = null,
        targetDeviceId: String? = null,
    ) {
        eventLogger.log(
            DevRelayEvent(
                occurredAtMillis = System.currentTimeMillis(),
                method = exchange.requestMethod,
                path = exchange.requestURI.path,
                remoteAddress = exchange.remoteAddress?.address?.hostAddress.orEmpty(),
                accepted = accepted,
                result = result,
                messageId = messageId,
                commandId = commandId,
                resultId = resultId,
                sourceDeviceId = sourceDeviceId,
                targetDeviceId = targetDeviceId,
            ),
        )
    }

    private fun HttpExchange.respond(
        statusCode: Int,
        text: String,
    ) {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        responseHeaders.set("Content-Type", "text/plain; charset=utf-8")
        sendResponseHeaders(statusCode, bytes.size.toLong())
        responseBody.use { output ->
            output.write(bytes)
        }
    }
}
