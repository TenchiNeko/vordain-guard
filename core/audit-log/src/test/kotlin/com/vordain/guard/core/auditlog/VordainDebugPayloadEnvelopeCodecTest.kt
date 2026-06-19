package com.vordain.guard.core.auditlog

import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class VordainDebugPayloadEnvelopeCodecTest {
    private val codec = VordainDebugPayloadEnvelopeCodec()

    @Test
    fun envelopeRoundTrips() {
        val encoded = codec.encode(
            VordainDebugPayloadEnvelope(
                kind = VordainDebugPayloadKind.POLICY_UPDATE,
                version = 1,
                createdAtMillis = 1234L,
                payloadText = "VORDAIN_DEBUG_POLICY_UPDATE_V1\npolicyVersion=debug-1",
            ),
        )

        val decoded = assertIs<VordainDebugPayloadEnvelopeCodecResult.Decoded>(codec.decode(encoded))
        assertEquals(VordainDebugPayloadKind.POLICY_UPDATE, decoded.envelope.kind)
        assertEquals(1, decoded.envelope.version)
        assertEquals(1234L, decoded.envelope.createdAtMillis)
        assertEquals("VORDAIN_DEBUG_POLICY_UPDATE_V1\npolicyVersion=debug-1", decoded.envelope.payloadText)
    }

    @Test
    fun rejectsWrongHeader() {
        assertIs<VordainDebugPayloadEnvelopeCodecResult.Rejected>(codec.decode("WRONG\nkind=POLICY_UPDATE"))
    }

    @Test
    fun rejectsUnknownKind() {
        val payload = """
            VORDAIN_DEBUG_PAYLOAD_ENVELOPE_V1
            kind=NOPE
            version=1
            createdAtMillis=1
            payloadText<<
            body
        """.trimIndent()

        assertIs<VordainDebugPayloadEnvelopeCodecResult.Rejected>(codec.decode(payload))
    }

    @Test
    fun sourceHasNoAndroidImportsOrManagerNames() {
        val source = Files.walk(Path("src/main"))
            .filter(Files::isRegularFile)
            .map { Files.readString(it) }
            .toList()
            .joinToString(separator = "\n")

        assertFalse(source.contains("android" + "."))
        assertFalse(Regex("""\b(class|object)\s+\w*Manager\b""").containsMatchIn(source))
    }
}
