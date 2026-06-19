package com.vordain.guard.vpn.service

data class TunPacketCaptureStats(
    val startedAtMillis: Long? = null,
    val stoppedAtMillis: Long? = null,
    val packetCount: Long = 0,
    val byteCount: Long = 0,
    val ipv4Count: Long = 0,
    val ipv6Count: Long = 0,
    val tcpCount: Long = 0,
    val udpCount: Long = 0,
    val icmpCount: Long = 0,
    val malformedCount: Long = 0,
    val lastPacketSummary: String? = null,
)

interface LabPacketCaptureSink {
    fun onCaptureStarted(startedAtMillis: Long)
    fun onPacket(metadata: TunPacketMetadata)
    fun onMalformedPacket(byteLength: Int)
    fun onCaptureStopped(stoppedAtMillis: Long)
    fun snapshot(): TunPacketCaptureStats
}

class InMemoryLabPacketCaptureSink : LabPacketCaptureSink {
    private val lock = Any()
    private var stats = TunPacketCaptureStats()

    override fun onCaptureStarted(startedAtMillis: Long) {
        synchronized(lock) {
            stats = TunPacketCaptureStats(startedAtMillis = startedAtMillis)
        }
    }

    override fun onPacket(metadata: TunPacketMetadata) {
        synchronized(lock) {
            stats = stats.copy(
                packetCount = stats.packetCount + 1,
                byteCount = stats.byteCount + metadata.byteLength,
                ipv4Count = stats.ipv4Count + if (metadata.ipVersion == 4) 1 else 0,
                ipv6Count = stats.ipv6Count + if (metadata.ipVersion == 6) 1 else 0,
                tcpCount = stats.tcpCount + if (metadata.protocolLabel == "TCP") 1 else 0,
                udpCount = stats.udpCount + if (metadata.protocolLabel == "UDP") 1 else 0,
                icmpCount = stats.icmpCount + if (metadata.protocolLabel.startsWith("ICMP")) 1 else 0,
                lastPacketSummary = metadata.asSummary(),
            )
        }
    }

    override fun onMalformedPacket(byteLength: Int) {
        synchronized(lock) {
            stats = stats.copy(
                packetCount = stats.packetCount + 1,
                byteCount = stats.byteCount + byteLength,
                malformedCount = stats.malformedCount + 1,
                lastPacketSummary = "Malformed packet ($byteLength bytes)",
            )
        }
    }

    override fun onCaptureStopped(stoppedAtMillis: Long) {
        synchronized(lock) {
            stats = stats.copy(stoppedAtMillis = stoppedAtMillis)
        }
    }

    override fun snapshot(): TunPacketCaptureStats {
        return synchronized(lock) { stats }
    }
}

object LabPacketCaptureDebugStatus {
    private val sink = InMemoryLabPacketCaptureSink()

    fun sink(): LabPacketCaptureSink = sink

    fun snapshot(): TunPacketCaptureStats = sink.snapshot()
}
