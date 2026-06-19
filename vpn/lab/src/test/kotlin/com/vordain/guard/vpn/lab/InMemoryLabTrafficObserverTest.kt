package com.vordain.guard.vpn.lab

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InMemoryLabTrafficObserverTest {
    @Test
    fun nonDnsPacketIncrementsPacketCountOnly() {
        val observer = InMemoryLabTrafficObserver()

        val stats = observer.observePacket(
            packet = ipv4UdpPacket(payload = byteArrayOf(1, 2, 3), sourcePort = 12_345, destinationPort = 123),
            length = 31,
            observedAtMillis = 1_000L,
        )

        assertEquals(1, stats.packetCount)
        assertEquals(0, stats.dnsPacketCount)
        assertEquals(0, stats.dnsQueryCount)
    }

    @Test
    fun dnsPacketIncrementsDnsPacketCount() {
        val observer = InMemoryLabTrafficObserver()

        val packet = dnsPacketFor("allowed.example")
        val stats = observer.observePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(1, stats.packetCount)
        assertEquals(1, stats.dnsPacketCount)
        assertEquals(1, stats.dnsQueryCount)
    }

    @Test
    fun dnsQueryDomainIsExtracted() {
        val observer = InMemoryLabTrafficObserver()

        val packet = dnsPacketFor("allowed.example")
        val stats = observer.observePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals("allowed.example", stats.lastDnsObservation?.queriedDomains?.single()?.value)
    }

    @Test
    fun sampleBlockedDomainEvaluatesBlocked() {
        val observer = InMemoryLabTrafficObserver()

        val packet = dnsPacketFor("blocked.example")
        val stats = observer.observePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(1, stats.blockedDomainCount)
        assertEquals("BLOCK", stats.lastDnsObservation?.decisions?.single()?.actionLabel)
        assertEquals("BLOCKLIST_MATCH", stats.lastDnsObservation?.decisions?.single()?.reasonLabel)
    }

    @Test
    fun sampleAllowedDomainEvaluatesAllowed() {
        val observer = InMemoryLabTrafficObserver()

        val packet = dnsPacketFor("allowed.example")
        val stats = observer.observePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(1, stats.allowedDomainCount)
        assertEquals("ALLOW", stats.lastDnsObservation?.decisions?.single()?.actionLabel)
    }

    @Test
    fun proxySampleEvaluatesBlocked() {
        val observer = InMemoryLabTrafficObserver()

        val packet = dnsPacketFor("proxy.test")
        val stats = observer.observePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(1, stats.blockedDomainCount)
        assertEquals("PROXY_CATEGORY_BLOCKED", stats.lastDnsObservation?.decisions?.single()?.reasonLabel)
    }

    @Test
    fun malformedDnsIncrementsMalformedDnsCount() {
        val observer = InMemoryLabTrafficObserver()
        val packet = ipv4UdpPacket(payload = byteArrayOf(1, 2, 3), sourcePort = 12_345, destinationPort = 53)

        val stats = observer.observePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(1, stats.dnsPacketCount)
        assertEquals(1, stats.malformedDnsCount)
    }

    @Test
    fun recentDnsObservationsAreCapped() {
        val observer = InMemoryLabTrafficObserver(recentObservationLimit = 3)

        listOf("one.example", "two.example", "three.example", "four.example").forEachIndexed { index, domain ->
            val packet = dnsPacketFor(domain)
            observer.observePacket(packet, packet.size, observedAtMillis = index.toLong())
        }

        val stats = observer.snapshot()
        assertEquals(3, stats.recentDnsObservations.size)
        assertEquals("four.example", stats.recentDnsObservations.first().queriedDomains.single().value)
        assertEquals("two.example", stats.recentDnsObservations.last().queriedDomains.single().value)
    }

    @Test
    fun statsDoNotRetainRawPacketBytes() {
        val observer = InMemoryLabTrafficObserver()
        val packet = dnsPacketFor("allowed.example")

        val stats = observer.observePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertFalse(stats.toString().contains("ByteArray"))
        assertFalse(stats.toString().contains(packet.joinToString(",")))
    }

    @Test
    fun sourceHasNoForbiddenDependenciesOrManagerNames() {
        val sourceRoot = repositoryRoot().resolve("vpn/lab/src/main")
        val source = sourceRoot.walkTopDown()
            .filter { it.isFile }
            .joinToString(separator = "\n") { it.readText() }

        assertFalse(source.contains("android" + "."))
        assertFalse(source.contains("backend"))
        assertFalse(source.contains("data.relay"))
        assertFalse(source.contains("data.outbox"))
        assertFalse(Regex("class .*" + "Man" + "ager|object .*" + "Man" + "ager").containsMatchIn(source))
        assertFalse(source.contains("TO" + "DO"))
        assertFalse(source.contains("FIX" + "ME"))
    }

    private fun dnsPacketFor(domain: String): ByteArray {
        return ipv4UdpPacket(payload = dnsQuery(domain), sourcePort = 12_345, destinationPort = 53)
    }

    private fun dnsQuery(domain: String): ByteArray {
        val labels = domain.split('.')
        val qnameLength = labels.sumOf { it.length + 1 } + 1
        val message = ByteArray(12 + qnameLength + 4)
        message[2] = 0x01
        message[5] = 0x01
        var offset = 12
        labels.forEach { label ->
            message[offset++] = label.length.toByte()
            label.encodeToByteArray().copyInto(message, destinationOffset = offset)
            offset += label.length
        }
        message[offset++] = 0
        message.writeUnsignedShort(offset, 1)
        message.writeUnsignedShort(offset + 2, 1)
        return message
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
