package com.vordain.guard.vpn.packet

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DnsUdpUpstreamResponseWrapperTest {
    private val wrapper = DnsUdpUpstreamResponseWrapper()

    @Test
    fun wrapsValidUpstreamDnsPayloadIntoIpv4UdpResponsePacket() {
        val original = dnsPacketFor("allowed.example")
        val upstreamPayload = dnsResponsePayload(transactionId = 0x4567)

        val result = wrapper.buildResponseFromUpstreamPayload(original, original.size, upstreamPayload)

        assertTrue(result.built)
        assertNotNull(result.responseBytes)
    }

    @Test
    fun responseSwapsSourceAndDestinationIpAddresses() {
        val original = dnsPacketFor("allowed.example")

        val response = wrapper.buildResponseFromUpstreamPayload(original, original.size, dnsResponsePayload())
            .responseBytes ?: error("response expected")

        assertEquals("10.0.0.2", response.ipv4Address(12))
        assertEquals("10.0.0.1", response.ipv4Address(16))
    }

    @Test
    fun responseSwapsSourceAndDestinationUdpPorts() {
        val original = dnsPacketFor("allowed.example", sourcePort = 44_000)

        val response = wrapper.buildResponseFromUpstreamPayload(original, original.size, dnsResponsePayload())
            .responseBytes ?: error("response expected")

        assertEquals(53, response.readUInt16(20))
        assertEquals(44_000, response.readUInt16(22))
    }

    @Test
    fun responseIncludesUpstreamDnsTransactionId() {
        val original = dnsPacketFor("allowed.example")

        val response = wrapper.buildResponseFromUpstreamPayload(
            originalPacket = original,
            originalLength = original.size,
            upstreamDnsPayload = dnsResponsePayload(transactionId = 0x4a9b),
        ).responseBytes ?: error("response expected")

        assertEquals(0x4a9b, response.readUInt16(28))
    }

    @Test
    fun responseRecalculatesPacketLengthFields() {
        val original = dnsPacketFor("allowed.example")
        val upstreamPayload = dnsResponsePayload(extraBytes = byteArrayOf(1, 2, 3, 4))

        val response = wrapper.buildResponseFromUpstreamPayload(original, original.size, upstreamPayload)
            .responseBytes ?: error("response expected")

        assertEquals(response.size, response.readUInt16(2))
        assertEquals(8 + upstreamPayload.size, response.readUInt16(24))
    }

    @Test
    fun nonDnsOriginalPacketReturnsNoResponse() {
        val original = ipv4UdpPacket(payload = byteArrayOf(1, 2, 3), sourcePort = 12_345, destinationPort = 123)

        val result = wrapper.buildResponseFromUpstreamPayload(original, original.size, dnsResponsePayload())

        assertFalse(result.built)
    }

    @Test
    fun malformedOriginalPacketReturnsNoResponse() {
        val result = wrapper.buildResponseFromUpstreamPayload(byteArrayOf(0x45), 1, dnsResponsePayload())

        assertFalse(result.built)
    }

    @Test
    fun tooShortUpstreamPayloadReturnsNoResponse() {
        val original = dnsPacketFor("allowed.example")

        val result = wrapper.buildResponseFromUpstreamPayload(original, original.size, ByteArray(11))

        assertFalse(result.built)
    }

    @Test
    fun ipv6OriginalPacketReturnsSafeNoResponse() {
        val original = ByteArray(48)
        original[0] = 0x60
        original[6] = 17
        original.putUInt16(40, 12_345)
        original.putUInt16(42, 53)

        val result = wrapper.buildResponseFromUpstreamPayload(original, original.size, dnsResponsePayload())

        assertFalse(result.built)
    }

    @Test
    fun sourceHasNoAndroidImportsOrManagerNames() {
        val sourceRoot = repositoryRoot().resolve("vpn/packet/src/main")
        val source = sourceRoot.walkTopDown()
            .filter { it.isFile }
            .joinToString(separator = "\n") { it.readText() }

        assertFalse(source.contains("android" + "."))
        assertFalse(Regex("class .*" + "Man" + "ager|object .*" + "Man" + "ager").containsMatchIn(source))
        assertFalse(source.contains("TO" + "DO"))
        assertFalse(source.contains("FIX" + "ME"))
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
        message.putUInt16(0, transactionId)
        message[2] = 0x01
        message[5] = 0x01
        var offset = 12
        labels.forEach { label ->
            message[offset++] = label.length.toByte()
            label.encodeToByteArray().copyInto(message, destinationOffset = offset)
            offset += label.length
        }
        message[offset++] = 0
        message.putUInt16(offset, 1)
        message.putUInt16(offset + 2, 1)
        return message
    }

    private fun dnsResponsePayload(
        transactionId: Int = 0x1234,
        extraBytes: ByteArray = byteArrayOf(0, 1),
    ): ByteArray {
        val response = ByteArray(12 + extraBytes.size)
        response.putUInt16(0, transactionId)
        response[2] = 0x81.toByte()
        response[3] = 0x80.toByte()
        response.putUInt16(4, 1)
        response.putUInt16(6, 0)
        extraBytes.copyInto(response, destinationOffset = 12)
        return response
    }

    private fun ipv4UdpPacket(payload: ByteArray, sourcePort: Int, destinationPort: Int): ByteArray {
        val packet = ByteArray(20 + 8 + payload.size)
        packet[0] = 0x45
        packet.putUInt16(2, packet.size)
        packet[8] = 64
        packet[9] = 17
        packet[12] = 10
        packet[15] = 1
        packet[16] = 10
        packet[19] = 2
        packet.putUInt16(20, sourcePort)
        packet.putUInt16(22, destinationPort)
        packet.putUInt16(24, 8 + payload.size)
        payload.copyInto(packet, destinationOffset = 28)
        return packet
    }

    private fun ByteArray.readUInt16(offset: Int): Int {
        return ((this[offset].toInt() and 0xff) shl 8) or (this[offset + 1].toInt() and 0xff)
    }

    private fun ByteArray.putUInt16(offset: Int, value: Int) {
        this[offset] = ((value ushr 8) and 0xff).toByte()
        this[offset + 1] = (value and 0xff).toByte()
    }

    private fun ByteArray.ipv4Address(offset: Int): String {
        return (0 until 4).joinToString(separator = ".") { index ->
            (this[offset + index].toInt() and 0xff).toString()
        }
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
