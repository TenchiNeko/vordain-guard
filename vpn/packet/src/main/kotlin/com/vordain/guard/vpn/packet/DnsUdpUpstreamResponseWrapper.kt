package com.vordain.guard.vpn.packet

class DnsUdpUpstreamResponseWrapper {
    fun buildResponseFromUpstreamPayload(
        originalPacket: ByteArray,
        originalLength: Int,
        upstreamDnsPayload: ByteArray,
    ): PacketResponseBuildResult {
        val safeLength = minOf(originalLength, originalPacket.size)
        if (safeLength < IPV4_MIN_HEADER_BYTES) {
            return noResponse("Original packet is shorter than IPv4 header")
        }
        val version = (originalPacket[0].unsigned() ushr 4) and 0x0f
        if (version != IPV4_VERSION) {
            return noResponse("Only IPv4 UDP DNS upstream responses are supported")
        }
        val headerLength = (originalPacket[0].unsigned() and 0x0f) * 4
        if (headerLength < IPV4_MIN_HEADER_BYTES || safeLength < headerLength + UDP_HEADER_BYTES) {
            return noResponse("Original IPv4 or UDP header is malformed")
        }
        val totalLength = originalPacket.readUInt16(IPV4_TOTAL_LENGTH_OFFSET)
        val effectiveLength = minOf(safeLength, totalLength)
        if (effectiveLength < headerLength + UDP_HEADER_BYTES) {
            return noResponse("Original IPv4 total length is shorter than UDP header")
        }
        if (originalPacket[IPV4_PROTOCOL_OFFSET].unsigned() != UDP_PROTOCOL) {
            return noResponse("Original packet is not UDP")
        }
        val udpOffset = headerLength
        val destinationPort = originalPacket.readUInt16(udpOffset + UDP_DESTINATION_PORT_OFFSET)
        if (destinationPort != DNS_PORT) {
            return noResponse("Original UDP packet is not a DNS query to port 53")
        }
        val udpLength = originalPacket.readUInt16(udpOffset + UDP_LENGTH_OFFSET)
        if (udpLength < UDP_HEADER_BYTES || effectiveLength < udpOffset + udpLength) {
            return noResponse("Original UDP length is malformed")
        }
        if (upstreamDnsPayload.size < DNS_HEADER_BYTES) {
            return noResponse("Upstream DNS response payload is shorter than header")
        }

        val responseLength = headerLength + UDP_HEADER_BYTES + upstreamDnsPayload.size
        val response = ByteArray(responseLength)
        originalPacket.copyInto(
            destination = response,
            destinationOffset = 0,
            startIndex = 0,
            endIndex = headerLength + UDP_HEADER_BYTES,
        )
        upstreamDnsPayload.copyInto(response, destinationOffset = headerLength + UDP_HEADER_BYTES)

        response.putUInt16(IPV4_TOTAL_LENGTH_OFFSET, responseLength)
        swapIpv4Addresses(response)
        swapUdpPorts(response, udpOffset)
        response.putUInt16(udpOffset + UDP_LENGTH_OFFSET, UDP_HEADER_BYTES + upstreamDnsPayload.size)
        response.putUInt16(udpOffset + UDP_CHECKSUM_OFFSET, 0)
        response.putUInt16(IPV4_HEADER_CHECKSUM_OFFSET, 0)
        response.putUInt16(IPV4_HEADER_CHECKSUM_OFFSET, ipv4HeaderChecksum(response, headerLength))

        return PacketResponseBuildResult(
            responseBytes = response,
            reason = "Built DNS upstream response packet",
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
    }
}
