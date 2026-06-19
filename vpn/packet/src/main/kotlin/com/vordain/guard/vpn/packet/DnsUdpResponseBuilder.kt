package com.vordain.guard.vpn.packet

enum class DnsBlockResponseMode {
    NXDOMAIN,
    IPV4_ZERO,
    REFUSED,
}

data class PacketResponseBuildResult(
    val responseBytes: ByteArray?,
    val reason: String,
    val built: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PacketResponseBuildResult) return false
        return responseBytes.contentEquals(other.responseBytes) &&
            reason == other.reason &&
            built == other.built
    }

    override fun hashCode(): Int {
        var result = responseBytes?.contentHashCode() ?: 0
        result = 31 * result + reason.hashCode()
        result = 31 * result + built.hashCode()
        return result
    }
}

class DnsUdpResponseBuilder {
    fun buildBlockedDnsResponse(
        packet: ByteArray,
        length: Int,
        responseMode: DnsBlockResponseMode,
    ): PacketResponseBuildResult {
        if (responseMode == DnsBlockResponseMode.IPV4_ZERO) {
            return noResponse("IPv4 zero-answer sinkhole response is not implemented")
        }
        val safeLength = minOf(length, packet.size)
        if (safeLength < IPV4_MIN_HEADER_BYTES) {
            return noResponse("Packet is shorter than IPv4 header")
        }
        val version = (packet[0].unsigned() ushr 4) and 0x0f
        if (version != IPV4_VERSION) {
            return noResponse("Only IPv4 UDP DNS responses are supported")
        }
        val headerLength = (packet[0].unsigned() and 0x0f) * 4
        if (headerLength < IPV4_MIN_HEADER_BYTES || safeLength < headerLength + UDP_HEADER_BYTES) {
            return noResponse("IPv4 or UDP header is malformed")
        }
        val totalLength = packet.readUInt16(IPV4_TOTAL_LENGTH_OFFSET)
        val effectiveLength = minOf(safeLength, totalLength)
        if (effectiveLength < headerLength + UDP_HEADER_BYTES) {
            return noResponse("IPv4 total length is shorter than UDP header")
        }
        if (packet[IPV4_PROTOCOL_OFFSET].unsigned() != UDP_PROTOCOL) {
            return noResponse("Packet is not UDP")
        }
        val udpOffset = headerLength
        val destinationPort = packet.readUInt16(udpOffset + UDP_DESTINATION_PORT_OFFSET)
        if (destinationPort != DNS_PORT) {
            return noResponse("UDP packet is not a DNS query to port 53")
        }
        val udpLength = packet.readUInt16(udpOffset + UDP_LENGTH_OFFSET)
        if (udpLength < UDP_HEADER_BYTES || effectiveLength < udpOffset + udpLength) {
            return noResponse("UDP length is malformed")
        }
        val dnsOffset = udpOffset + UDP_HEADER_BYTES
        val dnsLength = udpLength - UDP_HEADER_BYTES
        if (dnsLength < DNS_HEADER_BYTES) {
            return noResponse("DNS query is shorter than header")
        }

        val response = packet.copyOfRange(0, effectiveLength)
        swapIpv4Addresses(response)
        swapUdpPorts(response, udpOffset)
        response.putUInt16(udpOffset + UDP_CHECKSUM_OFFSET, 0)
        response[dnsOffset + DNS_FLAGS_HIGH_OFFSET] =
            ((response[dnsOffset + DNS_FLAGS_HIGH_OFFSET].unsigned() and DNS_FLAGS_HIGH_PRESERVE_MASK) or DNS_QR_RESPONSE)
                .toByte()
        response[dnsOffset + DNS_FLAGS_LOW_OFFSET] = when (responseMode) {
            DnsBlockResponseMode.NXDOMAIN -> DNS_RCODE_NXDOMAIN.toByte()
            DnsBlockResponseMode.REFUSED -> DNS_RCODE_REFUSED.toByte()
            DnsBlockResponseMode.IPV4_ZERO -> error("handled above")
        }
        response.putUInt16(dnsOffset + DNS_ANSWER_COUNT_OFFSET, 0)
        response.putUInt16(dnsOffset + DNS_AUTHORITY_COUNT_OFFSET, 0)
        response.putUInt16(dnsOffset + DNS_ADDITIONAL_COUNT_OFFSET, 0)
        response.putUInt16(IPV4_HEADER_CHECKSUM_OFFSET, 0)
        response.putUInt16(IPV4_HEADER_CHECKSUM_OFFSET, ipv4HeaderChecksum(response, headerLength))
        return PacketResponseBuildResult(
            responseBytes = response,
            reason = "Built ${responseMode.name} DNS block response",
            built = true,
        )
    }

    private fun swapIpv4Addresses(packet: ByteArray) {
        for (index in 0 until IPV4_ADDRESS_BYTES) {
            val sourceByte = packet[IPV4_SOURCE_ADDRESS_OFFSET + index]
            packet[IPV4_SOURCE_ADDRESS_OFFSET + index] = packet[IPV4_DESTINATION_ADDRESS_OFFSET + index]
            packet[IPV4_DESTINATION_ADDRESS_OFFSET + index] = sourceByte
        }
    }

    private fun swapUdpPorts(packet: ByteArray, udpOffset: Int) {
        val sourcePort = packet.readUInt16(udpOffset + UDP_SOURCE_PORT_OFFSET)
        val destinationPort = packet.readUInt16(udpOffset + UDP_DESTINATION_PORT_OFFSET)
        packet.putUInt16(udpOffset + UDP_SOURCE_PORT_OFFSET, destinationPort)
        packet.putUInt16(udpOffset + UDP_DESTINATION_PORT_OFFSET, sourcePort)
    }

    private fun ipv4HeaderChecksum(packet: ByteArray, headerLength: Int): Int {
        var sum = 0
        var offset = 0
        while (offset < headerLength) {
            sum += packet.readUInt16(offset)
            while (sum > 0xffff) {
                sum = (sum and 0xffff) + (sum ushr 16)
            }
            offset += 2
        }
        return sum.inv() and 0xffff
    }

    private fun noResponse(reason: String): PacketResponseBuildResult {
        return PacketResponseBuildResult(responseBytes = null, reason = reason, built = false)
    }

    private fun ByteArray.readUInt16(offset: Int): Int {
        return (this[offset].unsigned() shl 8) or this[offset + 1].unsigned()
    }

    private fun ByteArray.putUInt16(offset: Int, value: Int) {
        this[offset] = ((value ushr 8) and 0xff).toByte()
        this[offset + 1] = (value and 0xff).toByte()
    }

    private fun Byte.unsigned(): Int = toInt() and 0xff

    private companion object {
        const val IPV4_VERSION = 4
        const val IPV4_MIN_HEADER_BYTES = 20
        const val IPV4_TOTAL_LENGTH_OFFSET = 2
        const val IPV4_PROTOCOL_OFFSET = 9
        const val IPV4_HEADER_CHECKSUM_OFFSET = 10
        const val IPV4_SOURCE_ADDRESS_OFFSET = 12
        const val IPV4_DESTINATION_ADDRESS_OFFSET = 16
        const val IPV4_ADDRESS_BYTES = 4
        const val UDP_PROTOCOL = 17
        const val UDP_HEADER_BYTES = 8
        const val UDP_SOURCE_PORT_OFFSET = 0
        const val UDP_DESTINATION_PORT_OFFSET = 2
        const val UDP_LENGTH_OFFSET = 4
        const val UDP_CHECKSUM_OFFSET = 6
        const val DNS_PORT = 53
        const val DNS_HEADER_BYTES = 12
        const val DNS_FLAGS_HIGH_OFFSET = 2
        const val DNS_FLAGS_LOW_OFFSET = 3
        const val DNS_ANSWER_COUNT_OFFSET = 6
        const val DNS_AUTHORITY_COUNT_OFFSET = 8
        const val DNS_ADDITIONAL_COUNT_OFFSET = 10
        const val DNS_QR_RESPONSE = 0x80
        const val DNS_FLAGS_HIGH_PRESERVE_MASK = 0x79
        const val DNS_RCODE_NXDOMAIN = 0x03
        const val DNS_RCODE_REFUSED = 0x05
    }
}
