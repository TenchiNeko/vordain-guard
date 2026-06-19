package com.vordain.guard.vpn.service

object TunPacketMetadataParser {
    fun parse(
        packet: ByteArray,
        byteLength: Int,
    ): TunPacketMetadata? {
        if (byteLength <= 0 || packet.isEmpty()) {
            return null
        }
        val version = (packet[0].toInt() ushr 4) and 0x0f
        return when (version) {
            4 -> parseIpv4(packet, byteLength)
            6 -> parseIpv6(packet, byteLength)
            else -> null
        }
    }

    private fun parseIpv4(
        packet: ByteArray,
        byteLength: Int,
    ): TunPacketMetadata? {
        if (byteLength < 20) {
            return null
        }
        val headerLength = (packet[0].toInt() and 0x0f) * 4
        if (headerLength < 20 || byteLength < headerLength) {
            return null
        }
        val protocol = packet[9].unsigned()
        val sourceAddress = ipv4Address(packet, 12)
        val destinationAddress = ipv4Address(packet, 16)
        val ports = transportPorts(packet, byteLength, headerLength, protocol)
        return TunPacketMetadata(
            ipVersion = 4,
            protocolLabel = protocolLabel(protocol),
            sourceAddress = sourceAddress,
            destinationAddress = destinationAddress,
            sourcePort = ports?.first,
            destinationPort = ports?.second,
            byteLength = byteLength,
        )
    }

    private fun parseIpv6(
        packet: ByteArray,
        byteLength: Int,
    ): TunPacketMetadata? {
        if (byteLength < 40) {
            return null
        }
        val protocol = packet[6].unsigned()
        val sourceAddress = ipv6Address(packet, 8)
        val destinationAddress = ipv6Address(packet, 24)
        val ports = transportPorts(packet, byteLength, 40, protocol)
        return TunPacketMetadata(
            ipVersion = 6,
            protocolLabel = protocolLabel(protocol),
            sourceAddress = sourceAddress,
            destinationAddress = destinationAddress,
            sourcePort = ports?.first,
            destinationPort = ports?.second,
            byteLength = byteLength,
        )
    }

    private fun transportPorts(
        packet: ByteArray,
        byteLength: Int,
        offset: Int,
        protocol: Int,
    ): Pair<Int, Int>? {
        if (protocol != TCP_PROTOCOL && protocol != UDP_PROTOCOL) {
            return null
        }
        if (byteLength < offset + 4) {
            return null
        }
        return packet.readUnsignedShort(offset) to packet.readUnsignedShort(offset + 2)
    }

    private fun protocolLabel(protocol: Int): String {
        return when (protocol) {
            TCP_PROTOCOL -> "TCP"
            UDP_PROTOCOL -> "UDP"
            ICMP_PROTOCOL -> "ICMP"
            ICMPV6_PROTOCOL -> "ICMPv6"
            else -> "Protocol $protocol"
        }
    }

    private fun ipv4Address(packet: ByteArray, offset: Int): String {
        return (0 until 4)
            .joinToString(separator = ".") { index -> packet[offset + index].unsigned().toString() }
    }

    private fun ipv6Address(packet: ByteArray, offset: Int): String {
        return (0 until 8)
            .joinToString(separator = ":") { index ->
                val groupOffset = offset + (index * 2)
                packet.readUnsignedShort(groupOffset).toString(16)
            }
    }

    private fun ByteArray.readUnsignedShort(offset: Int): Int {
        return (this[offset].unsigned() shl 8) or this[offset + 1].unsigned()
    }

    private fun Byte.unsigned(): Int = toInt() and 0xff

    private const val TCP_PROTOCOL = 6
    private const val UDP_PROTOCOL = 17
    private const val ICMP_PROTOCOL = 1
    private const val ICMPV6_PROTOCOL = 58
}
