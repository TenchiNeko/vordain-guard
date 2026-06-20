package com.vordain.guard.core.devrelay

import com.vordain.guard.core.model.DeviceId
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DevRelayDebugCommandCodecTest {
    private val codec = DevRelayDebugCommandCodec()

    @Test
    fun commandRoundTrips() {
        val command = command()

        val decoded = codec.decodeCommand(codec.encodeCommand(command))

        assertTrue(decoded.accepted)
        assertEquals(command, decoded.command)
    }

    @Test
    fun commandsRoundTrip() {
        val commands = listOf(
            command("command-1"),
            command("command-2", type = DevRelayDebugCommandType.CHILD_SEND_STATUS_BUNDLE),
        )

        val decoded = codec.decodeCommands(codec.encodeCommands(commands))

        assertTrue(decoded.accepted)
        assertEquals(commands, decoded.commands)
    }

    @Test
    fun resultRoundTrips() {
        val result = result()

        val decoded = codec.decodeResult(codec.encodeResult(result))

        assertTrue(decoded.accepted)
        assertEquals(result, decoded.result)
    }

    @Test
    fun resultsRoundTrip() {
        val results = listOf(
            result("result-1"),
            result("result-2", success = false, summary = "wrong direction"),
        )

        val decoded = codec.decodeResults(codec.encodeResults(results))

        assertTrue(decoded.accepted)
        assertEquals(results, decoded.results)
    }

    @Test
    fun wrongHeaderRejected() {
        val decoded = codec.decodeCommand("WRONG\ncommandId=command-1")

        assertFalse(decoded.accepted)
        assertNull(decoded.command)
    }

    @Test
    fun unknownCommandTypeRejected() {
        val encoded = codec.encodeCommand(command()).replace("type=PARENT_RELAY_HEALTH_CHECK", "type=RUN_ANYTHING")

        val decoded = codec.decodeCommand(encoded)

        assertFalse(decoded.accepted)
    }

    @Test
    fun missingSourceRejected() {
        val encoded = codec.encodeCommand(command()).replace("sourceDeviceId=server-debug-device", "sourceDeviceId=")

        val decoded = codec.decodeCommand(encoded)

        assertFalse(decoded.accepted)
    }

    @Test
    fun forbiddenSensitiveFieldRejected() {
        val decoded = codec.decodeCommand(
            """
            VORDAIN_DEBUG_REMOTE_TEST_COMMAND_V1
            commandId=command-1
            type=PARENT_RELAY_HEALTH_CHECK
            sourceDeviceId=server-debug-device
            targetDeviceId=parent-debug-device
            createdAtMillis=1
            status=PENDING
            pinValue=1234
            """.trimIndent(),
        )

        assertFalse(decoded.accepted)
    }

    @Test
    fun releaseRemoteTestControllersAreNoOpStubs() {
        val parentRelease = Files.readString(
            Path.of("../../apps/parent-app/src/release/java/com/vordain/guard/parent/ParentRemoteTestController.kt"),
        )
        val childRelease = Files.readString(
            Path.of("../../apps/child-app/src/release/java/com/vordain/guard/child/ChildRemoteTestController.kt"),
        )

        assertTrue(parentRelease.contains("No-op release implementation"))
        assertTrue(childRelease.contains("No-op release implementation"))
        assertFalse(parentRelease.contains("fetchCommands"))
        assertFalse(childRelease.contains("fetchCommands"))
        assertFalse(parentRelease.contains("Thread"))
        assertFalse(childRelease.contains("Thread"))
    }

    @Test
    fun sourceHasNoForbiddenImportsOrMarkers() {
        val sourceRoot = Path.of("src/main/kotlin/com/vordain/guard/core/devrelay")
        val source = Files.walk(sourceRoot)
            .filter { Files.isRegularFile(it) }
            .map { Files.readString(it) }
            .toList()
            .joinToString(separator = "\n")

        assertFalse(source.contains("android" + "."))
        assertFalse(source.contains("backend"))
        assertFalse(source.contains("data.relay"))
        assertFalse(source.contains("data.outbox"))
        assertFalse(source.contains("class DevRelayManager"))
        assertFalse(source.contains("object DevRelayManager"))
        assertFalse(source.contains("TO" + "DO"))
        assertFalse(source.contains("FIX" + "ME"))
    }

    private fun command(
        id: String = "command-1",
        type: DevRelayDebugCommandType = DevRelayDebugCommandType.PARENT_RELAY_HEALTH_CHECK,
    ): DevRelayDebugCommand {
        return DevRelayDebugCommand(
            commandId = id,
            type = type,
            sourceDeviceId = DeviceId("server-debug-device"),
            targetDeviceId = DeviceId("parent-debug-device"),
            createdAtMillis = 123L,
            status = DevRelayDebugCommandStatus.PENDING,
        )
    }

    private fun result(
        id: String = "result-1",
        success: Boolean = true,
        summary: String = "ok",
    ): DevRelayDebugCommandResult {
        return DevRelayDebugCommandResult(
            resultId = id,
            commandId = "command-1",
            type = DevRelayDebugCommandType.PARENT_RELAY_HEALTH_CHECK,
            sourceDeviceId = DeviceId("parent-debug-device"),
            targetDeviceId = DeviceId("server-debug-device"),
            createdAtMillis = 124L,
            success = success,
            summary = summary,
        )
    }
}
