package com.vordain.guard.core.auditlog

enum class VordainDebugPayloadKind {
    PAIRING_INVITE,
    PAIRING_ACCEPTANCE,
    POLICY_UPDATE,
    SETUP_REPORT,
    BYPASS_REPORT,
    CHILD_STATUS_REPORT,
    DIAGNOSTICS,
}

data class VordainDebugPayloadEnvelope(
    val kind: VordainDebugPayloadKind,
    val version: Int,
    val createdAtMillis: Long,
    val payloadText: String,
)

sealed class VordainDebugPayloadEnvelopeCodecResult {
    data class Decoded(val envelope: VordainDebugPayloadEnvelope) : VordainDebugPayloadEnvelopeCodecResult()
    data class Rejected(val reason: String) : VordainDebugPayloadEnvelopeCodecResult()
}

class VordainDebugPayloadEnvelopeCodec {
    fun encode(envelope: VordainDebugPayloadEnvelope): String {
        return listOf(
            HEADER,
            "kind=${envelope.kind.name}",
            "version=${envelope.version}",
            "createdAtMillis=${envelope.createdAtMillis}",
            PAYLOAD_MARKER,
            envelope.payloadText,
        ).joinToString(separator = "\n")
    }

    fun decode(text: String): VordainDebugPayloadEnvelopeCodecResult {
        val lines = text.lines()
        if (lines.firstOrNull()?.trim() != HEADER) {
            return VordainDebugPayloadEnvelopeCodecResult.Rejected("Wrong header")
        }
        val markerIndex = lines.indexOf(PAYLOAD_MARKER)
        if (markerIndex < 0) {
            return VordainDebugPayloadEnvelopeCodecResult.Rejected("Missing payload marker")
        }
        val values = lines
            .drop(1)
            .take(markerIndex - 1)
            .mapNotNull { line ->
                val index = line.indexOf('=')
                if (index <= 0) {
                    null
                } else {
                    line.substring(0, index).trim() to line.substring(index + 1).trim()
                }
            }
            .toMap()
        val kind = values["kind"]?.let { value ->
            enumValues<VordainDebugPayloadKind>().firstOrNull { it.name == value }
        } ?: return VordainDebugPayloadEnvelopeCodecResult.Rejected("Unknown kind")
        val version = values["version"]?.toIntOrNull()
            ?: return VordainDebugPayloadEnvelopeCodecResult.Rejected("Malformed version")
        val createdAtMillis = values["createdAtMillis"]?.toLongOrNull()
            ?: return VordainDebugPayloadEnvelopeCodecResult.Rejected("Malformed createdAtMillis")
        return VordainDebugPayloadEnvelopeCodecResult.Decoded(
            VordainDebugPayloadEnvelope(
                kind = kind,
                version = version,
                createdAtMillis = createdAtMillis,
                payloadText = lines.drop(markerIndex + 1).joinToString(separator = "\n"),
            ),
        )
    }

    companion object {
        const val HEADER = "VORDAIN_DEBUG_PAYLOAD_ENVELOPE_V1"
        private const val PAYLOAD_MARKER = "payloadText<<"
    }
}
