package com.vordain.guard.vpn.packet

enum class IpVersion {
    IPV4,
    IPV6,
    UNKNOWN,
}

enum class TransportProtocol {
    TCP,
    UDP,
    ICMP,
    ICMPV6,
    OTHER,
    UNKNOWN,
}

data class ParsedPacketMetadata(
    val ipVersion: IpVersion,
    val protocol: TransportProtocol,
    val sourceAddress: String?,
    val destinationAddress: String?,
    val sourcePort: Int?,
    val destinationPort: Int?,
    val byteLength: Int,
    val malformed: Boolean,
    val malformedReason: String?,
) {
    fun summary(): String {
        if (malformed) {
            return "Malformed packet: ${malformedReason ?: "unknown"} ($byteLength bytes)"
        }
        val endpoints = if (sourceAddress != null && destinationAddress != null) {
            val source = if (sourcePort != null) "$sourceAddress:$sourcePort" else sourceAddress
            val destination = if (destinationPort != null) "$destinationAddress:$destinationPort" else destinationAddress
            "$source -> $destination"
        } else {
            "metadata unavailable"
        }
        return "$ipVersion $protocol $endpoints ($byteLength bytes)"
    }
}

data class ParsedUdpPayload(
    val metadata: ParsedPacketMetadata,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParsedUdpPayload) return false
        return metadata == other.metadata && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = metadata.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

data class PacketParseResult(
    val metadata: ParsedPacketMetadata,
    val udpPayload: ParsedUdpPayload?,
)
