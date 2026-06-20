package com.vordain.guard.core.syncbundle

import com.vordain.guard.core.model.DeviceId
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncBundleCodecTest {
    private val codec = SyncBundleCodec()

    @Test
    fun childToParentBundleRoundTrips() {
        val bundle = childBundle()

        val decoded = codec.decode(codec.encode(bundle))

        assertTrue(decoded.accepted)
        assertEquals(bundle, decoded.bundle)
    }

    @Test
    fun parentToChildBundleRoundTrips() {
        val bundle = parentBundle()

        val decoded = codec.decode(codec.encode(bundle))

        assertTrue(decoded.accepted)
        assertEquals(bundle, decoded.bundle)
    }

    @Test
    fun multiplePayloadsRoundTrip() {
        val bundle = childBundle().copy(
            payloads = listOf(
                SyncBundlePayload(SyncBundlePayloadKind.CHILD_SECURITY_STATUS_REPORT, "status", "status payload"),
                SyncBundlePayload(SyncBundlePayloadKind.CHILD_ALERT_REPORT, "alerts", "alert payload"),
                SyncBundlePayload(SyncBundlePayloadKind.AUDIT_SUMMARY, "audit", "audit payload"),
            ),
        )

        val decoded = codec.decode(codec.encode(bundle))

        assertTrue(decoded.accepted)
        assertEquals(3, decoded.bundle?.payloads?.size)
    }

    @Test
    fun wrongHeaderRejected() {
        val decoded = codec.decode("NOPE\nbundleId=b1")

        assertFalse(decoded.accepted)
        assertNull(decoded.bundle)
    }

    @Test
    fun unknownPayloadKindRejected() {
        val encoded = codec.encode(childBundle())
            .replace("payload.0.kind=CHILD_SECURITY_STATUS_REPORT", "payload.0.kind=UNKNOWN")

        val decoded = codec.decode(encoded)

        assertFalse(decoded.accepted)
    }

    @Test
    fun missingSourceDeviceIdRejected() {
        val encoded = codec.encode(childBundle())
            .replace("sourceDeviceId=child-debug-device", "sourceDeviceId=")

        val decoded = codec.decode(encoded)

        assertFalse(decoded.accepted)
    }

    @Test
    fun forbiddenSensitiveFieldRejected() {
        val decoded = codec.decode(
            """
            VORDAIN_DEBUG_SYNC_BUNDLE_V1
            bundleId=b1
            direction=CHILD_TO_PARENT
            kind=CHILD_STATUS_EXPORT
            createdAtMillis=123
            sourceDeviceId=child-debug-device
            pinValue=1234
            payloadCount=0
            """.trimIndent(),
        )

        assertFalse(decoded.accepted)
    }

    @Test
    fun warningSaysEncryptedRelayLater() {
        assertEquals(
            "Local debug bundle only. Production sync will use encrypted relay later.",
            SyncBundle.WARNING_TEXT,
        )
    }

    @Test
    fun sourceHasNoForbiddenImportsOrMarkers() {
        val sourceRoot = Path.of("src/main/kotlin/com/vordain/guard/core/syncbundle")
        val source = Files.walk(sourceRoot)
            .filter { Files.isRegularFile(it) }
            .map { Files.readString(it) }
            .toList()
            .joinToString(separator = "\n")

        assertFalse(source.contains("android" + "."))
        assertFalse(source.contains("backend"))
        assertFalse(source.contains("data.relay"))
        assertFalse(source.contains("data.outbox"))
        assertFalse(source.contains("class SyncBundleManager"))
        assertFalse(source.contains("object SyncBundleManager"))
        assertFalse(source.contains("TO" + "DO"))
        assertFalse(source.contains("FIX" + "ME"))
        assertFalse(source.contains("child " + "activity"))
        assertFalse(source.contains("traffic " + "log"))
    }

    @Test
    fun decodedPayloadTextIsPreserved() {
        val text = "line 1\nline=2\nsymbols %"
        val bundle = childBundle().copy(
            payloads = listOf(
                SyncBundlePayload(SyncBundlePayloadKind.DIAGNOSTICS_TEXT, "diagnostics", text),
            ),
        )

        val decoded = codec.decode(codec.encode(bundle))

        assertTrue(decoded.accepted)
        assertEquals(text, decoded.bundle?.payloads?.single()?.payloadText)
        assertNotNull(decoded.bundle?.payloads?.single()?.label)
    }

    private fun childBundle(): SyncBundle {
        return SyncBundle(
            bundleId = "child-bundle-1",
            direction = SyncBundleDirection.CHILD_TO_PARENT,
            kind = SyncBundleKind.CHILD_STATUS_EXPORT,
            createdAtMillis = 123L,
            sourceDeviceId = DeviceId("child-debug-device"),
            targetDeviceId = DeviceId("parent-debug-device"),
            payloads = listOf(
                SyncBundlePayload(SyncBundlePayloadKind.CHILD_SECURITY_STATUS_REPORT, "status", "status payload"),
            ),
        )
    }

    private fun parentBundle(): SyncBundle {
        return SyncBundle(
            bundleId = "parent-bundle-1",
            direction = SyncBundleDirection.PARENT_TO_CHILD,
            kind = SyncBundleKind.PARENT_POLICY_UPDATE,
            createdAtMillis = 456L,
            sourceDeviceId = DeviceId("parent-debug-device"),
            targetDeviceId = DeviceId("child-debug-device"),
            payloads = listOf(
                SyncBundlePayload(SyncBundlePayloadKind.POLICY_UPDATE, "policy", "policy payload"),
            ),
        )
    }
}
