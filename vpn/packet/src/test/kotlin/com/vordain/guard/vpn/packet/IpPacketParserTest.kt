package com.vordain.guard.vpn.packet

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IpPacketParserTest {
    private val parser = IpPacketParser()

    @Test
    fun parsesMinimalIpv4UdpPacket() {
        val packet = ipv4UdpPacket(payload = byteArrayOf(1, 2, 3), sourcePort = 12_345, destinationPort = 53)

        val result = parser.parse(packet, packet.size)

        assertFalse(result.metadata.malformed)
        assertEquals(IpVersion.IPV4, result.metadata.ipVersion)
        assertEquals(TransportProtocol.UDP, result.metadata.protocol)
        assertEquals("10.0.0.1", result.metadata.sourceAddress)
        assertEquals("10.0.0.2", result.metadata.destinationAddress)
        assertEquals(12_345, result.metadata.sourcePort)
        assertEquals(53, result.metadata.destinationPort)
    }

    @Test
    fun parsesIpv4UdpPayload() {
        val payload = byteArrayOf(0x12, 0x34, 0x01, 0x00)
        val packet = ipv4UdpPacket(payload = payload, sourcePort = 12_345, destinationPort = 53)

        val result = parser.parse(packet, packet.size)

        assertNotNull(result.udpPayload)
        assertTrue(result.udpPayload.payload.contentEquals(payload))
    }

    @Test
    fun parsesMinimalIpv4TcpPacket() {
        val packet = ipv4TcpPacket(sourcePort = 443, destinationPort = 55_000)

        val result = parser.parse(packet, packet.size)

        assertFalse(result.metadata.malformed)
        assertEquals(TransportProtocol.TCP, result.metadata.protocol)
        assertEquals(443, result.metadata.sourcePort)
        assertEquals(55_000, result.metadata.destinationPort)
        assertEquals(null, result.udpPayload)
    }

    @Test
    fun parsesMinimalIpv6UdpPacket() {
        val packet = ipv6UdpPacket(payload = byteArrayOf(9, 8, 7), sourcePort = 1111, destinationPort = 53)

        val result = parser.parse(packet, packet.size)

        assertFalse(result.metadata.malformed)
        assertEquals(IpVersion.IPV6, result.metadata.ipVersion)
        assertEquals(TransportProtocol.UDP, result.metadata.protocol)
        assertEquals(1111, result.metadata.sourcePort)
        assertEquals(53, result.metadata.destinationPort)
        assertNotNull(result.udpPayload)
    }

    @Test
    fun shortPacketBecomesMalformed() {
        val result = parser.parse(byteArrayOf(0x45), 1)

        assertTrue(result.metadata.malformed)
    }

    @Test
    fun invalidIpv4HeaderLengthBecomesMalformed() {
        val packet = ByteArray(20)
        packet[0] = 0x41

        val result = parser.parse(packet, packet.size)

        assertTrue(result.metadata.malformed)
    }

    @Test
    fun shortUdpLengthBecomesMalformed() {
        val packet = ipv4UdpPacket(payload = byteArrayOf(), sourcePort = 1, destinationPort = 2)
        packet.writeUnsignedShort(24, 7)

        val result = parser.parse(packet, packet.size)

        assertTrue(result.metadata.malformed)
    }

    @Test
    fun sourceHasNoAndroidImportsManagerNamesOrTodos() {
        val sourceRoot = repositoryRoot().resolve("vpn/packet/src/main")
        val source = sourceRoot.walkTopDown()
            .filter { it.isFile }
            .joinToString(separator = "\n") { it.readText() }

        assertFalse(source.contains("android" + "."))
        assertFalse(Regex("class .*" + "Man" + "ager|object .*" + "Man" + "ager").containsMatchIn(source))
        assertFalse(source.contains("TO" + "DO"))
        assertFalse(source.contains("FIX" + "ME"))
    }

    private fun ipv4UdpPacket(payload: ByteArray, sourcePort: Int, destinationPort: Int): ByteArray {
        val packet = ByteArray(20 + 8 + payload.size)
        packet[0] = 0x45
        packet.writeUnsignedShort(2, packet.size)
        packet[9] = 17
        packet[12] = 10
        packet[15] = 1
        packet[16] = 10
        packet[19] = 2
        packet.writeUnsignedShort(20, sourcePort)
        packet.writeUnsignedShort(22, destinationPort)
        packet.writeUnsignedShort(24, 8 + payload.size)
        payload.copyInto(packet, destinationOffset = 28)
        return packet
    }

    private fun ipv4TcpPacket(sourcePort: Int, destinationPort: Int): ByteArray {
        val packet = ByteArray(24)
        packet[0] = 0x45
        packet.writeUnsignedShort(2, packet.size)
        packet[9] = 6
        packet[12] = 10
        packet[15] = 1
        packet[16] = 10
        packet[19] = 2
        packet.writeUnsignedShort(20, sourcePort)
        packet.writeUnsignedShort(22, destinationPort)
        return packet
    }

    private fun ipv6UdpPacket(payload: ByteArray, sourcePort: Int, destinationPort: Int): ByteArray {
        val packet = ByteArray(40 + 8 + payload.size)
        packet[0] = 0x60
        packet[6] = 17
        packet[39] = 1
        packet.writeUnsignedShort(40, sourcePort)
        packet.writeUnsignedShort(42, destinationPort)
        packet.writeUnsignedShort(44, 8 + payload.size)
        payload.copyInto(packet, destinationOffset = 48)
        return packet
    }

    private fun ByteArray.writeUnsignedShort(offset: Int, value: Int) {
        this[offset] = ((value ushr 8) and 0xff).toByte()
        this[offset + 1] = (value and 0xff).toByte()
    }

    private fun repositoryRoot(): File {
        val userDir = System.getProperty("user.dir") ?: "."
        var current = File(userDir).absoluteFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) {
                return current
            }
            current = current.parentFile ?: error("Could not find repository root")
        }
    }
}
