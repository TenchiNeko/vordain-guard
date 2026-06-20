package com.vordain.guard.backend.relayapi

import com.vordain.guard.core.devrelay.DevRelayDirection
import com.vordain.guard.core.devrelay.DevRelayDebugCommand
import com.vordain.guard.core.devrelay.DevRelayDebugCommandCodec
import com.vordain.guard.core.devrelay.DevRelayDebugCommandResult
import com.vordain.guard.core.devrelay.DevRelayDebugCommandStatus
import com.vordain.guard.core.devrelay.DevRelayDebugCommandType
import com.vordain.guard.core.devrelay.DevRelayMessage
import com.vordain.guard.core.devrelay.DevRelayMessageCodec
import com.vordain.guard.core.devrelay.DevRelayMessageStatus
import com.vordain.guard.core.model.DeviceId
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DevRelayHttpServerTest {
    private val codec = DevRelayMessageCodec()
    private val commandCodec = DevRelayDebugCommandCodec()

    @Test
    fun storeAcceptsAndQueriesMessage() {
        val store = InMemoryDevRelayStore()
        val message = message()

        store.put(message)
        val queried = store.query(com.vordain.guard.core.devrelay.DevRelayInboxQuery(DeviceId("child-debug-device")))

        assertEquals(1, queried.size)
        assertEquals(DevRelayMessageStatus.FETCHED, queried.single().status)
    }

    @Test
    fun acknowledgeChangesStatus() {
        val store = InMemoryDevRelayStore()
        val message = message()

        store.put(message)
        assertTrue(store.acknowledge(message.messageId))

        assertEquals(1, store.stats().acknowledgedCount)
    }

    @Test
    fun wrongTargetDoesNotReturnMessage() {
        val store = InMemoryDevRelayStore()
        store.put(message())

        val queried = store.query(com.vordain.guard.core.devrelay.DevRelayInboxQuery(DeviceId("other-device")))

        assertTrue(queried.isEmpty())
    }

    @Test
    fun malformedMessageIsRejectedByCodec() {
        val decoded = codec.decodeMessage("bad")

        assertFalse(decoded.accepted)
    }

    @Test
    fun healthEndpointReturnsOk() {
        val server = DevRelayHttpServer(
            bindHost = "127.0.0.1",
            port = 0,
            eventLogger = DevRelayEventLogger(Files.createTempFile("vordain-relay-health", ".ndjson")),
        )
        val port = server.start()
        try {
            val connection = URI("http://127.0.0.1:$port/health").toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            assertEquals(200, connection.responseCode)
            assertEquals("OK", connection.inputStream.readBytes().toString(StandardCharsets.UTF_8))
        } finally {
            server.stop()
        }
    }

    @Test
    fun debugCommandEndpointQueuesAndReturnsCommand() {
        val server = DevRelayHttpServer(
            bindHost = "127.0.0.1",
            port = 0,
            eventLogger = DevRelayEventLogger(Files.createTempFile("vordain-relay-command", ".ndjson")),
        )
        val port = server.start()
        try {
            val post = URI("http://127.0.0.1:$port/debug/v1/test-commands").toURL().openConnection() as HttpURLConnection
            post.requestMethod = "POST"
            post.doOutput = true
            post.outputStream.use { output ->
                output.write(commandCodec.encodeCommand(command()).toByteArray(StandardCharsets.UTF_8))
            }
            assertEquals(202, post.responseCode)

            val get = URI("http://127.0.0.1:$port/debug/v1/test-commands?targetDeviceId=parent-debug-device")
                .toURL()
                .openConnection() as HttpURLConnection
            get.requestMethod = "GET"

            assertEquals(200, get.responseCode)
            val decoded = commandCodec.decodeCommands(get.inputStream.readBytes().toString(StandardCharsets.UTF_8))
            assertTrue(decoded.accepted)
            assertEquals(1, decoded.commands.size)
            assertEquals(DevRelayDebugCommandStatus.FETCHED, decoded.commands.single().status)
        } finally {
            server.stop()
        }
    }

    @Test
    fun debugResultEndpointStoresResultAndWritesMetadataLog() {
        val logPath = Files.createTempFile("vordain-relay-result", ".ndjson")
        val server = DevRelayHttpServer(
            bindHost = "127.0.0.1",
            port = 0,
            eventLogger = DevRelayEventLogger(logPath),
        )
        val port = server.start()
        try {
            val post = URI("http://127.0.0.1:$port/debug/v1/test-results").toURL().openConnection() as HttpURLConnection
            post.requestMethod = "POST"
            post.doOutput = true
            post.outputStream.use { output ->
                output.write(commandCodec.encodeResult(commandResult()).toByteArray(StandardCharsets.UTF_8))
            }
            assertEquals(202, post.responseCode)

            val get = URI("http://127.0.0.1:$port/debug/v1/test-results?targetDeviceId=server-debug-device")
                .toURL()
                .openConnection() as HttpURLConnection
            get.requestMethod = "GET"

            assertEquals(200, get.responseCode)
            val decoded = commandCodec.decodeResults(get.inputStream.readBytes().toString(StandardCharsets.UTF_8))
            assertTrue(decoded.accepted)
            assertEquals(1, decoded.results.size)

            val log = Files.readString(logPath)
            assertTrue(log.contains("\"path\":\"/debug/v1/test-results\""))
            assertTrue(log.contains("\"resultId\":\"result-1\""))
            assertFalse(log.contains("bundleText"))
        } finally {
            server.stop()
        }
    }

    @Test
    fun sourceHasNoForbiddenMarkers() {
        val sourceRoot = Path.of("src/main/kotlin/com/vordain/guard/backend/relayapi")
        val source = Files.walk(sourceRoot)
            .filter { Files.isRegularFile(it) }
            .map { Files.readString(it) }
            .toList()
            .joinToString(separator = "\n")

        assertFalse(source.contains("class RelayManager"))
        assertFalse(source.contains("object RelayManager"))
        assertFalse(source.contains("TO" + "DO"))
        assertFalse(source.contains("FIX" + "ME"))
        assertFalse(source.contains("child " + "activity"))
        assertFalse(source.contains("traffic " + "log"))
    }

    private fun message(): DevRelayMessage {
        return DevRelayMessage(
            messageId = "message-1",
            direction = DevRelayDirection.PARENT_TO_CHILD,
            sourceDeviceId = DeviceId("parent-debug-device"),
            targetDeviceId = DeviceId("child-debug-device"),
            createdAtMillis = 123L,
            bundleText = "bundle",
            status = DevRelayMessageStatus.PENDING,
        )
    }

    private fun command(): DevRelayDebugCommand {
        return DevRelayDebugCommand(
            commandId = "command-1",
            type = DevRelayDebugCommandType.PARENT_RELAY_HEALTH_CHECK,
            sourceDeviceId = DeviceId("server-debug-device"),
            targetDeviceId = DeviceId("parent-debug-device"),
            createdAtMillis = 123L,
            status = DevRelayDebugCommandStatus.PENDING,
        )
    }

    private fun commandResult(): DevRelayDebugCommandResult {
        return DevRelayDebugCommandResult(
            resultId = "result-1",
            commandId = "command-1",
            type = DevRelayDebugCommandType.PARENT_RELAY_HEALTH_CHECK,
            sourceDeviceId = DeviceId("parent-debug-device"),
            targetDeviceId = DeviceId("server-debug-device"),
            createdAtMillis = 124L,
            success = true,
            summary = "ok",
        )
    }
}
