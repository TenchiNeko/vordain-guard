package com.vordain.guard.vpn.lab

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.policy.Policy
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
    fun blockedDnsDomainReturnsWriteDnsBlockResponse() {
        val observer = InMemoryLabTrafficObserver()
        val packet = dnsPacketFor("blocked.example")

        val result = observer.handlePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(LabPacketAction.WRITE_DNS_BLOCK_RESPONSE, result.action)
        assertNotNull(result.responseBytes)
        assertEquals(1, result.stats.dnsBlockedResponseCount)
    }

    @Test
    fun allowedDnsDomainReturnsDropBecauseForwardingIsNotImplemented() {
        val observer = InMemoryLabTrafficObserver()
        val packet = dnsPacketFor("allowed.example")

        val result = observer.handlePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(LabPacketAction.DROP, result.action)
        assertEquals(null, result.responseBytes)
        assertEquals(1, result.stats.dnsAllowedDroppedCount)
        assertTrue(result.decisionSummary.orEmpty().contains("forwarding disabled"))
    }

    @Test
    fun blockedDnsDomainDoesNotCallUpstreamTransport() {
        val upstream = RecordingUpstreamTransport(LabDnsUpstreamResult.success(dnsResponsePayload()))
        val observer = InMemoryLabTrafficObserver(
            initialForwardingMode = LabDnsForwardingMode.LAB_UPSTREAM,
            initialUpstreamTransport = upstream,
        )
        val packet = dnsPacketFor("blocked.example")

        val result = observer.handlePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(LabPacketAction.WRITE_DNS_BLOCK_RESPONSE, result.action)
        assertEquals(0, upstream.callCount)
    }

    @Test
    fun allowedDnsDomainCallsUpstreamTransport() {
        val upstream = RecordingUpstreamTransport(LabDnsUpstreamResult.success(dnsResponsePayload()))
        val observer = InMemoryLabTrafficObserver(
            initialForwardingMode = LabDnsForwardingMode.LAB_UPSTREAM,
            initialUpstreamTransport = upstream,
        )
        val packet = dnsPacketFor("allowed.example")

        observer.handlePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(1, upstream.callCount)
        assertEquals("1.1.1.1", upstream.lastQuery?.upstreamHost)
        assertEquals(53, upstream.lastQuery?.upstreamPort)
    }

    @Test
    fun allowedDnsWithUpstreamSuccessReturnsResponseBytes() {
        val observer = InMemoryLabTrafficObserver(
            initialForwardingMode = LabDnsForwardingMode.LAB_UPSTREAM,
            initialUpstreamTransport = RecordingUpstreamTransport(LabDnsUpstreamResult.success(dnsResponsePayload())),
        )
        val packet = dnsPacketFor("allowed.example")

        val result = observer.handlePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(LabPacketAction.WRITE_DNS_UPSTREAM_RESPONSE, result.action)
        assertNotNull(result.responseBytes)
        assertEquals(1, result.stats.dnsAllowedForwardedCount)
    }

    @Test
    fun dnsOnlyBlockedDnsWritesBlockResponseAndCountsDnsOnlyBlock() {
        val observer = InMemoryLabTrafficObserver(
            initialForwardingMode = LabDnsForwardingMode.LAB_UPSTREAM,
            initialUpstreamTransport = RecordingUpstreamTransport(LabDnsUpstreamResult.success(dnsResponsePayload())),
        )
        observer.configureUpstream(
            forwardingMode = LabDnsForwardingMode.LAB_UPSTREAM,
            upstreamTransport = RecordingUpstreamTransport(LabDnsUpstreamResult.success(dnsResponsePayload())),
            captureMode = LabCaptureMode.DNS_ONLY_LAB,
        )
        val packet = dnsPacketFor("blocked.example")

        val result = observer.handlePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(LabPacketAction.WRITE_DNS_BLOCK_RESPONSE, result.action)
        assertEquals(1, result.stats.dnsOnlyBlockedResponseCount)
    }

    @Test
    fun dnsOnlyAllowedDnsForwardsUpstreamAndCountsDnsOnlyForward() {
        val observer = InMemoryLabTrafficObserver()
        observer.configureUpstream(
            forwardingMode = LabDnsForwardingMode.LAB_UPSTREAM,
            upstreamTransport = RecordingUpstreamTransport(LabDnsUpstreamResult.success(dnsResponsePayload())),
            captureMode = LabCaptureMode.DNS_ONLY_LAB,
        )
        val packet = dnsPacketFor("allowed.example")

        val result = observer.handlePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(LabPacketAction.WRITE_DNS_UPSTREAM_RESPONSE, result.action)
        assertEquals(1, result.stats.dnsOnlyAllowedForwardedCount)
    }

    @Test
    fun dnsOnlyNonDnsIsDroppedAndCountedAsUnexpected() {
        val observer = InMemoryLabTrafficObserver()
        observer.configureUpstream(
            forwardingMode = LabDnsForwardingMode.LAB_UPSTREAM,
            upstreamTransport = RecordingUpstreamTransport(LabDnsUpstreamResult.success(dnsResponsePayload())),
            captureMode = LabCaptureMode.DNS_ONLY_LAB,
        )
        val packet = ipv4UdpPacket(payload = byteArrayOf(1, 2, 3), sourcePort = 12_345, destinationPort = 123)

        val result = observer.handlePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(LabPacketAction.DROP, result.action)
        assertEquals(1, result.stats.dnsOnlyUnexpectedNonDnsCount)
    }

    @Test
    fun dnsOnlyStatsAreSeparateFromFullTunnelStats() {
        val observer = InMemoryLabTrafficObserver()
        val packet = ipv4UdpPacket(payload = byteArrayOf(1, 2, 3), sourcePort = 12_345, destinationPort = 123)

        val fullTunnelStats = observer.handlePacket(packet, packet.size, observedAtMillis = 1_000L).stats
        observer.configureUpstream(
            forwardingMode = LabDnsForwardingMode.LAB_UPSTREAM,
            upstreamTransport = RecordingUpstreamTransport(LabDnsUpstreamResult.success(dnsResponsePayload())),
            captureMode = LabCaptureMode.DNS_ONLY_LAB,
        )
        val dnsOnlyStats = observer.handlePacket(packet, packet.size, observedAtMillis = 2_000L).stats

        assertEquals(1, fullTunnelStats.fullTunnelLabPacketCount)
        assertEquals(0, fullTunnelStats.dnsOnlyLabPacketCount)
        assertEquals(1, dnsOnlyStats.fullTunnelLabPacketCount)
        assertEquals(1, dnsOnlyStats.dnsOnlyLabPacketCount)
    }

    @Test
    fun allowedDnsWithUpstreamFailureReturnsDropAndIncrementsFailureCount() {
        val observer = InMemoryLabTrafficObserver(
            initialForwardingMode = LabDnsForwardingMode.LAB_UPSTREAM,
            initialUpstreamTransport = RecordingUpstreamTransport(LabDnsUpstreamResult.failure("timeout waiting for DNS")),
        )
        val packet = dnsPacketFor("allowed.example")

        val result = observer.handlePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(LabPacketAction.DROP, result.action)
        assertEquals(1, result.stats.dnsAllowedForwardFailureCount)
        assertEquals(1, result.stats.dnsAllowedForwardTimeoutCount)
    }

    @Test
    fun alertOnlyDnsBehaviorIsDroppedInLabMode() {
        val observer = InMemoryLabTrafficObserver(initialPolicy = monitorPolicy())
        val packet = dnsPacketFor("unknown.example")

        val result = observer.handlePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(LabPacketAction.DROP, result.action)
        assertEquals(1, result.stats.dnsAlertDroppedCount)
        assertTrue(result.decisionSummary.orEmpty().contains("Alert-only DNS"))
    }

    @Test
    fun nonDnsPacketReturnsDrop() {
        val observer = InMemoryLabTrafficObserver()
        val packet = ipv4UdpPacket(payload = byteArrayOf(1, 2, 3), sourcePort = 12_345, destinationPort = 123)

        val result = observer.handlePacket(packet, packet.size, observedAtMillis = 1_000L)

        assertEquals(LabPacketAction.DROP, result.action)
        assertEquals(0, result.stats.dnsPacketCount)
    }

    @Test
    fun malformedPacketReturnsDrop() {
        val observer = InMemoryLabTrafficObserver()

        val result = observer.handlePacket(byteArrayOf(0x45), 1, observedAtMillis = 1_000L)

        assertEquals(LabPacketAction.DROP, result.action)
        assertEquals(1, result.stats.malformedPacketCount)
    }

    @Test
    fun writeFailureCounterCanBeIncrementedByCaptureLoop() {
        val observer = InMemoryLabTrafficObserver()

        val stats = observer.markDnsResponseWriteFailure(observedAtMillis = 1_000L)

        assertEquals(1, stats.dnsResponseWriteFailureCount)
    }

    @Test
    fun writeSuccessCounterCanBeIncrementedByCaptureLoop() {
        val observer = InMemoryLabTrafficObserver()

        val stats = observer.markDnsResponseWriteSuccess(observedAtMillis = 1_000L)

        assertEquals(1, stats.dnsResponseWriteSuccessCount)
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

    private fun dnsResponsePayload(transactionId: Int = 0x1234): ByteArray {
        val response = ByteArray(12)
        response.writeUnsignedShort(0, transactionId)
        response[2] = 0x81.toByte()
        response[3] = 0x80.toByte()
        response.writeUnsignedShort(4, 1)
        return response
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

    private fun monitorPolicy(): Policy {
        return Policy(
            id = PolicyId("monitor-lab-policy"),
            mode = LockdownMode.MONITOR_ONLY,
            allowedDomains = emptySet<DomainName>(),
            blockedDomains = emptySet<DomainName>(),
            allowedPackages = emptySet<AppPackageName>(),
            blockedPackages = emptySet<AppPackageName>(),
            blockUnknownDomains = false,
            blockKnownProxyDomains = false,
        )
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

    private class RecordingUpstreamTransport(
        private val result: LabDnsUpstreamResult,
    ) : LabDnsUpstreamTransport {
        var callCount: Int = 0
            private set
        var lastQuery: LabDnsUpstreamQuery? = null
            private set

        override fun query(query: LabDnsUpstreamQuery): LabDnsUpstreamResult {
            callCount += 1
            lastQuery = query
            return result
        }
    }
}
