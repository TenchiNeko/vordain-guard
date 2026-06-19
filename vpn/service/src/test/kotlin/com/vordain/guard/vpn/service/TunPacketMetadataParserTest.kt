package com.vordain.guard.vpn.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TunPacketMetadataParserTest {
    @Test
    fun parsesMinimalIpv4UdpPacket() {
        val packet = ipv4Packet(protocol = 17, sourcePort = 12_345, destinationPort = 53)

        val metadata = assertNotNull(TunPacketMetadataParser.parse(packet, packet.size))

        assertEquals(4, metadata.ipVersion)
        assertEquals("UDP", metadata.protocolLabel)
        assertEquals("10.0.0.1", metadata.sourceAddress)
        assertEquals("10.0.0.2", metadata.destinationAddress)
        assertEquals(12_345, metadata.sourcePort)
        assertEquals(53, metadata.destinationPort)
    }

    @Test
    fun parsesMinimalIpv4TcpPacket() {
        val packet = ipv4Packet(protocol = 6, sourcePort = 443, destinationPort = 55_000)

        val metadata = assertNotNull(TunPacketMetadataParser.parse(packet, packet.size))

        assertEquals("TCP", metadata.protocolLabel)
        assertEquals(443, metadata.sourcePort)
        assertEquals(55_000, metadata.destinationPort)
    }

    @Test
    fun malformedShortPacketDoesNotParse() {
        assertNull(TunPacketMetadataParser.parse(byteArrayOf(0x45), 1))
    }

    @Test
    fun statsIncrementCountsWithoutPayloadStorage() {
        val sink = InMemoryLabPacketCaptureSink()
        val metadata = assertNotNull(TunPacketMetadataParser.parse(
            ipv4Packet(protocol = 17, sourcePort = 1000, destinationPort = 1001),
            24,
        ))

        sink.onCaptureStarted(100L)
        sink.onPacket(metadata)
        sink.onMalformedPacket(3)
        sink.onCaptureStopped(200L)

        val stats = sink.snapshot()
        assertEquals(2, stats.packetCount)
        assertEquals(27, stats.byteCount)
        assertEquals(1, stats.ipv4Count)
        assertEquals(1, stats.udpCount)
        assertEquals(1, stats.malformedCount)
        assertEquals(100L, stats.startedAtMillis)
        assertEquals(200L, stats.stoppedAtMillis)
        assertFalse(stats.toString().contains("payload", ignoreCase = true))
    }

    private fun ipv4Packet(
        protocol: Int,
        sourcePort: Int,
        destinationPort: Int,
    ): ByteArray {
        return ByteArray(24).also { packet ->
            packet[0] = 0x45
            packet[9] = protocol.toByte()
            packet[12] = 10
            packet[13] = 0
            packet[14] = 0
            packet[15] = 1
            packet[16] = 10
            packet[17] = 0
            packet[18] = 0
            packet[19] = 2
            packet.writeUnsignedShort(20, sourcePort)
            packet.writeUnsignedShort(22, destinationPort)
        }
    }

    private fun ByteArray.writeUnsignedShort(offset: Int, value: Int) {
        this[offset] = ((value ushr 8) and 0xff).toByte()
        this[offset + 1] = (value and 0xff).toByte()
    }
}
