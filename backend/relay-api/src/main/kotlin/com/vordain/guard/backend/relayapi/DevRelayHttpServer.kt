package com.vordain.guard.backend.relayapi

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
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
) {
    private var server: HttpServer? = null

    fun start(): Int {
        val httpServer = HttpServer.create(InetSocketAddress(bindHost, port), 0)
        httpServer.executor = Executors.newCachedThreadPool()
        httpServer.createContext("/health") { exchange ->
            if (exchange.requestMethod != "GET") {
                exchange.respond(405, "Method not allowed")
            } else {
                exchange.respond(200, "OK")
            }
        }
        httpServer.createContext("/debug/v1/messages") { exchange ->
            when (exchange.requestMethod) {
                "POST" -> postMessage(exchange)
                "GET" -> getMessages(exchange)
                else -> exchange.respond(405, "Method not allowed")
            }
        }
        httpServer.createContext("/debug/v1/ack") { exchange ->
            if (exchange.requestMethod == "POST") {
                ackMessage(exchange)
            } else {
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
                    ).joinToString(separator = "\n"),
                )
            } else {
                exchange.respond(405, "Method not allowed")
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
            exchange.respond(400, "rejected=${decoded.reason}")
            return
        }
        store.put(message)
        exchange.respond(202, "accepted=${message.messageId}")
    }

    private fun getMessages(exchange: HttpExchange) {
        val params = queryParams(exchange.requestURI.rawQuery.orEmpty())
        val targetDeviceId = params["targetDeviceId"]?.takeIf(String::isNotBlank)
        if (targetDeviceId == null) {
            exchange.respond(400, "Missing targetDeviceId")
            return
        }
        val messages = store.query(DevRelayInboxQuery(targetDeviceId = DeviceId(targetDeviceId)))
        exchange.respond(200, codec.encodeMessages(messages))
    }

    private fun ackMessage(exchange: HttpExchange) {
        val params = queryParams(exchange.requestURI.rawQuery.orEmpty())
        val messageId = params["messageId"]?.takeIf(String::isNotBlank)
        if (messageId == null) {
            exchange.respond(400, "Missing messageId")
            return
        }
        val acknowledged = store.acknowledge(messageId)
        if (acknowledged) {
            exchange.respond(200, "acknowledged=$messageId")
        } else {
            exchange.respond(404, "notFound=$messageId")
        }
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
