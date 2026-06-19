package com.vordain.guard.vpn.packet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DnsUdpResponseBuilderTest {
    private val builder = DnsUdpResponseBuilder()

    @Test
    fun buildsNxdomainResponseForValidIpv4UdpDnsQuery() {
        val packet = dnsPacketFor("blocked.example")

        val result = builder.buildBlockedDnsResponse(packet, packet.size, DnsBlockResponseMode.NXDOMAIN)

        assertTrue(result.built)
        assertNotNull(result.responseBytes)
    }

    @Test
    fun responseSwapsSourceAndDestinationIpAddresses() {
        val packet = dnsPacketFor("blocked.example")

        val response = builder.buildBlockedDnsResponse(packet, packet.size, DnsBlockResponseMode.NXDOMAIN)
            .responseBytes ?: error("response expected")

        assertEquals("10.0.0.2", response.ipv4Address(12))
        assertEquals("10.0.0.1", response.ipv4Address(16))
    }

    @Test
    fun responseSwapsSourceAndDestinationUdpPorts() {
        val packet = dnsPacketFor("blocked.example", sourcePort = 44_000)

        val response = builder.buildBlockedDnsResponse(packet, packet.size, DnsBlockResponseMode.NXDOMAIN)
            .responseBytes ?: error("response expected")

        assertEquals(53, response.readUInt16(20))
        assertEquals(44_000, response.readUInt16(22))
    }

    @Test
    fun responsePreservesDnsTransactionId() {
        val packet = dnsPacketFor("blocked.example", transactionId = 0x4a9b)

        val response = builder.buildBlockedDnsResponse(packet, packet.size, DnsBlockResponseMode.NXDOMAIN)
            .responseBytes ?: error("response expected")

        assertEquals(0x4a9b, response.readUInt16(28))
    }

    @Test
    fun responseSetsQrResponseBit() {
        val packet = dnsPacketFor("blocked.example")

        val response = builder.buildBlockedDnsResponse(packet, packet.size, DnsBlockResponseMode.NXDOMAIN)
            .responseBytes ?: error("response expected")

        assertTrue((response[30].toInt() and 0x80) != 0)
    }

    @Test
    fun responseUsesNxdomainRcode() {
        val packet = dnsPacketFor("blocked.example")

        val response = builder.buildBlockedDnsResponse(packet, packet.size, DnsBlockResponseMode.NXDOMAIN)
            .responseBytes ?: error("response expected")

        assertEquals(3, response[31].toInt() and 0x0f)
        assertEquals(0, response.readUInt16(34))
    }

    @Test
    fun nonDnsUdpPacketReturnsNoResponse() {
        val packet = ipv4UdpPacket(payload = byteArrayOf(1, 2, 3), sourcePort = 12_345, destinationPort = 123)

        val result = builder.buildBlockedDnsResponse(packet, packet.size, DnsBlockResponseMode.NXDOMAIN)

        assertFalse(result.built)
    }

    @Test
    fun tcpPacketReturnsNoResponse() {
        val packet = ipv4TcpPacket(sourcePort = 12_345, destinationPort = 53)

        val result = builder.buildBlockedDnsResponse(packet, packet.size, DnsBlockResponseMode.NXDOMAIN)

        assertFalse(result.built)
    }

    @Test
    fun malformedPacketReturnsNoResponse() {
        val result = builder.buildBlockedDnsResponse(byteArrayOf(0x45), 1, DnsBlockResponseMode.NXDOMAIN)

        assertFalse(result.built)
    }

    @Test
    fun ipv6PacketReturnsSafeNoResponse() {
        val packet = ByteArray(48)
        packet[0] = 0x60
        packet[6] = 17
        packet.writeUInt16(40, 12_345)
        packet.writeUInt16(42, 53)

        val result = builder.buildBlockedDnsResponse(packet, packet.size, DnsBlockResponseMode.NXDOMAIN)

        assertFalse(result.built)
    }

    @Test
    fun refusedResponseUsesRefusedRcode() {
        val packet = dnsPacketFor("blocked.example")

        val response = builder.buildBlockedDnsResponse(packet, packet.size, DnsBlockResponseMode.REFUSED)
            .responseBytes ?: error("response expected")

        assertEquals(5, response[31].toInt() and 0x0f)
    }

    private fun dnsPacketFor(
        domain: String,
        sourcePort: Int = 12_345,
        transactionId: Int = 0x1234,
    ): ByteArray {
        return ipv4UdpPacket(
            payload = dnsQuery(domain, transactionId),
            sourcePort = sourcePort,
            destinationPort = 53,
        )
    }

    private fun dnsQuery(domain: String, transactionId: Int): ByteArray {
        val labels = domain.split('.')
        val qnameLength = labels.sumOf { it.length + 1 } + 1
        val message = ByteArray(12 + qnameLength + 4)
        message.writeUInt16(0, transactionId)
        message[2] = 0x01
        message[5] = 0x01
        var offset = 12
        labels.forEach { label ->
            message[offset++] = label.length.toByte()
            label.encodeToByteArray().copyInto(message, destinationOffset = offset)
            offset += label.length
        }
        message[offset++] = 0
        message.writeUInt16(offset, 1)
        message.writeUInt16(offset + 2, 1)
        return message
    }

    private fun ipv4UdpPacket(payload: ByteArray, sourcePort: Int, destinationPort: Int): ByteArray {
        val packet = ByteArray(20 + 8 + payload.size)
        packet[0] = 0x45
        packet.writeUInt16(2, packet.size)
        packet[8] = 64
        packet[9] = 17
        packet[12] = 10
        packet[15] = 1
        packet[16] = 10
        packet[19] = 2
        packet.writeUInt16(20, sourcePort)
        packet.writeUInt16(22, destinationPort)
        packet.writeUInt16(24, 8 + payload.size)
        payload.copyInto(packet, destinationOffset = 28)
        return packet
    }

    private fun ipv4TcpPacket(sourcePort: Int, destinationPort: Int): ByteArray {
        val packet = ByteArray(24)
        packet[0] = 0x45
        packet.writeUInt16(2, packet.size)
        packet[9] = 6
        packet[12] = 10
        packet[15] = 1
        packet[16] = 10
        packet[19] = 2
        packet.writeUInt16(20, sourcePort)
        packet.writeUInt16(22, destinationPort)
        return packet
    }

    private fun ByteArray.readUInt16(offset: Int): Int {
        return ((this[offset].toInt() and 0xff) shl 8) or (this[offset + 1].toInt() and 0xff)
    }

    private fun ByteArray.writeUInt16(offset: Int, value: Int) {
        this[offset] = ((value ushr 8) and 0xff).toByte()
        this[offset + 1] = (value and 0xff).toByte()
    }

    private fun ByteArray.ipv4Address(offset: Int): String {
        return (0 until 4).joinToString(separator = ".") { index ->
            (this[offset + index].toInt() and 0xff).toString()
        }
    }
}
