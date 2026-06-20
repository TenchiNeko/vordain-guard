package com.vordain.guard.core.devrelay

import com.vordain.guard.core.model.DeviceId
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DevRelayMessageCodecTest {
    private val codec = DevRelayMessageCodec()

    @Test
    fun messageRoundTrips() {
        val message = message()

        val decoded = codec.decodeMessage(codec.encodeMessage(message))

        assertTrue(decoded.accepted)
        assertEquals(message, decoded.message)
    }

    @Test
    fun multipleMessagesRoundTrip() {
        val messages = listOf(
            message("m1"),
            message("m2", direction = DevRelayDirection.CHILD_TO_PARENT),
        )

        val decoded = codec.decodeMessages(codec.encodeMessages(messages))

        assertTrue(decoded.accepted)
        assertEquals(messages, decoded.messages)
    }

    @Test
    fun missingTargetRejected() {
        val encoded = codec.encodeMessage(message()).replace("targetDeviceId=child-debug-device", "targetDeviceId=")

        val decoded = codec.decodeMessage(encoded)

        assertFalse(decoded.accepted)
        assertNull(decoded.message)
    }

    @Test
    fun wrongHeaderRejected() {
        val decoded = codec.decodeMessage("WRONG\nmessageId=m1")

        assertFalse(decoded.accepted)
        assertNull(decoded.message)
    }

    @Test
    fun forbiddenSensitiveFieldRejected() {
        val decoded = codec.decodeMessage(
            """
            VORDAIN_DEBUG_DEV_RELAY_MESSAGE_V1
            messageId=m1
            direction=PARENT_TO_CHILD
            sourceDeviceId=parent-debug-device
            targetDeviceId=child-debug-device
            createdAtMillis=100
            status=PENDING
            pinValue=1234
            bundleText=bundle
            """.trimIndent(),
        )

        assertFalse(decoded.accepted)
    }

    @Test
    fun bundleTextIsPreserved() {
        val message = message(bundleText = "VORDAIN_DEBUG_SYNC_BUNDLE_V1\nbundleId=b1\npayload=line=two")

        val decoded = codec.decodeMessage(codec.encodeMessage(message))

        assertEquals(message.bundleText, decoded.message?.bundleText)
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
        assertFalse(source.contains("class DevRelayManager"))
        assertFalse(source.contains("object DevRelayManager"))
        assertFalse(source.contains("TO" + "DO"))
        assertFalse(source.contains("FIX" + "ME"))
    }

    private fun message(
        id: String = "message-1",
        direction: DevRelayDirection = DevRelayDirection.PARENT_TO_CHILD,
        bundleText: String = "bundle text",
    ): DevRelayMessage {
        return DevRelayMessage(
            messageId = id,
            direction = direction,
            sourceDeviceId = DeviceId("parent-debug-device"),
            targetDeviceId = DeviceId("child-debug-device"),
            createdAtMillis = 1234L,
            bundleText = bundleText,
            status = DevRelayMessageStatus.PENDING,
        )
    }
}
