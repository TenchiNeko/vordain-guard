package com.vordain.guard.vpn.packet

class IpPacketParser {
    fun parse(
        packet: ByteArray,
        length: Int,
    ): PacketParseResult {
        if (length <= 0 || packet.isEmpty()) {
            return malformed(length.coerceAtLeast(0), "packet is empty")
        }
        val safeLength = minOf(length, packet.size)
        val version = (packet[0].toInt() ushr 4) and 0x0f
        return when (version) {
            4 -> parseIpv4(packet, safeLength)
            6 -> parseIpv6(packet, safeLength)
            else -> malformed(safeLength, "unknown IP version $version")
        }
    }

    private fun parseIpv4(
        packet: ByteArray,
        length: Int,
    ): PacketParseResult {
        if (length < IPV4_MIN_HEADER_BYTES) {
            return malformed(length, "IPv4 packet shorter than header")
        }
        val headerLength = (packet[0].unsigned() and 0x0f) * 4
        if (headerLength < IPV4_MIN_HEADER_BYTES) {
            return malformed(length, "IPv4 header length is invalid")
        }
        if (length < headerLength) {
            return malformed(length, "IPv4 packet shorter than header length")
        }
        val totalLength = packet.readUnsignedShort(2)
        if (totalLength < headerLength) {
            return malformed(length, "IPv4 total length is shorter than header")
        }
        val effectiveLength = minOf(length, totalLength)
        val protocolNumber = packet[9].unsigned()
        val protocol = transportProtocol(protocolNumber, ipv6 = false)
        val sourceAddress = ipv4Address(packet, 12)
        val destinationAddress = ipv4Address(packet, 16)
        return parseTransport(
            packet = packet,
            length = effectiveLength,
            transportOffset = headerLength,
            ipVersion = IpVersion.IPV4,
            protocol = protocol,
            sourceAddress = sourceAddress,
            destinationAddress = destinationAddress,
        )
    }

    private fun parseIpv6(
        packet: ByteArray,
        length: Int,
    ): PacketParseResult {
        if (length < IPV6_BASE_HEADER_BYTES) {
            return malformed(length, "IPv6 packet shorter than base header")
        }
        val nextHeader = packet[6].unsigned()
        val protocol = transportProtocol(nextHeader, ipv6 = true)
        val sourceAddress = ipv6Address(packet, 8)
        val destinationAddress = ipv6Address(packet, 24)
        return parseTransport(
            packet = packet,
            length = length,
            transportOffset = IPV6_BASE_HEADER_BYTES,
            ipVersion = IpVersion.IPV6,
            protocol = protocol,
            sourceAddress = sourceAddress,
            destinationAddress = destinationAddress,
        )
    }

    private fun parseTransport(
        packet: ByteArray,
        length: Int,
        transportOffset: Int,
        ipVersion: IpVersion,
        protocol: TransportProtocol,
        sourceAddress: String,
        destinationAddress: String,
    ): PacketParseResult {
        if (protocol != TransportProtocol.UDP && protocol != TransportProtocol.TCP) {
            return PacketParseResult(
                metadata = metadata(
                    ipVersion = ipVersion,
                    protocol = protocol,
                    sourceAddress = sourceAddress,
                    destinationAddress = destinationAddress,
                    sourcePort = null,
                    destinationPort = null,
                    byteLength = length,
                ),
                udpPayload = null,
            )
        }
        if (length < transportOffset + 4) {
            return malformed(length, "$protocol header shorter than ports", ipVersion, protocol)
        }
        val sourcePort = packet.readUnsignedShort(transportOffset)
        val destinationPort = packet.readUnsignedShort(transportOffset + 2)
        val baseMetadata = metadata(
            ipVersion = ipVersion,
            protocol = protocol,
            sourceAddress = sourceAddress,
            destinationAddress = destinationAddress,
            sourcePort = sourcePort,
            destinationPort = destinationPort,
            byteLength = length,
        )
        if (protocol == TransportProtocol.TCP) {
            return PacketParseResult(metadata = baseMetadata, udpPayload = null)
        }
        if (length < transportOffset + UDP_HEADER_BYTES) {
            return malformed(length, "UDP header is too short", ipVersion, protocol)
        }
        val udpLength = packet.readUnsignedShort(transportOffset + 4)
        if (udpLength < UDP_HEADER_BYTES) {
            return malformed(length, "UDP length is too short", ipVersion, protocol)
        }
        if (length < transportOffset + udpLength) {
            return malformed(length, "UDP payload is truncated", ipVersion, protocol)
        }
        val payloadOffset = transportOffset + UDP_HEADER_BYTES
        val payloadLength = udpLength - UDP_HEADER_BYTES
        val udpPayload = packet.copyOfRange(payloadOffset, payloadOffset + payloadLength)
        return PacketParseResult(
            metadata = baseMetadata,
            udpPayload = ParsedUdpPayload(metadata = baseMetadata, payload = udpPayload),
        )
    }

    private fun metadata(
        ipVersion: IpVersion,
        protocol: TransportProtocol,
        sourceAddress: String?,
        destinationAddress: String?,
        sourcePort: Int?,
        destinationPort: Int?,
        byteLength: Int,
    ): ParsedPacketMetadata {
        return ParsedPacketMetadata(
            ipVersion = ipVersion,
            protocol = protocol,
            sourceAddress = sourceAddress,
            destinationAddress = destinationAddress,
            sourcePort = sourcePort,
            destinationPort = destinationPort,
            byteLength = byteLength,
            malformed = false,
            malformedReason = null,
        )
    }

    private fun malformed(
        byteLength: Int,
        reason: String,
        ipVersion: IpVersion = IpVersion.UNKNOWN,
        protocol: TransportProtocol = TransportProtocol.UNKNOWN,
    ): PacketParseResult {
        return PacketParseResult(
            metadata = ParsedPacketMetadata(
                ipVersion = ipVersion,
                protocol = protocol,
                sourceAddress = null,
                destinationAddress = null,
                sourcePort = null,
                destinationPort = null,
                byteLength = byteLength,
                malformed = true,
                malformedReason = reason,
            ),
            udpPayload = null,
        )
    }

    private fun transportProtocol(number: Int, ipv6: Boolean): TransportProtocol {
        return when (number) {
            TCP_PROTOCOL -> TransportProtocol.TCP
            UDP_PROTOCOL -> TransportProtocol.UDP
            ICMP_PROTOCOL -> if (ipv6) TransportProtocol.OTHER else TransportProtocol.ICMP
            ICMPV6_PROTOCOL -> TransportProtocol.ICMPV6
            else -> TransportProtocol.OTHER
        }
    }

    private fun ipv4Address(packet: ByteArray, offset: Int): String {
        return (0 until 4).joinToString(separator = ".") { index ->
            packet[offset + index].unsigned().toString()
        }
    }

    private fun ipv6Address(packet: ByteArray, offset: Int): String {
        return (0 until 8).joinToString(separator = ":") { index ->
            val groupOffset = offset + (index * 2)
            packet.readUnsignedShort(groupOffset).toString(16)
        }
    }

    private fun ByteArray.readUnsignedShort(offset: Int): Int {
        return (this[offset].unsigned() shl 8) or this[offset + 1].unsigned()
    }

    private fun Byte.unsigned(): Int = toInt() and 0xff

    private companion object {
        const val IPV4_MIN_HEADER_BYTES = 20
        const val IPV6_BASE_HEADER_BYTES = 40
        const val UDP_HEADER_BYTES = 8
        const val TCP_PROTOCOL = 6
        const val UDP_PROTOCOL = 17
        const val ICMP_PROTOCOL = 1
        const val ICMPV6_PROTOCOL = 58
    }
}
