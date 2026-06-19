package com.vordain.guard.vpn.service

import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class AndroidTunPacketCaptureLoop(
    private val descriptor: ParcelFileDescriptor,
    private val sink: LabPacketCaptureSink,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null

    fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }
        sink.onCaptureStarted(clock())
        thread = Thread(::capturePackets, "VordainLabTunCapture").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        if (!running.getAndSet(false)) {
            return
        }
        thread?.interrupt()
        thread = null
        sink.onCaptureStopped(clock())
    }

    private fun capturePackets() {
        val buffer = ByteArray(MAX_PACKET_BYTES)
        try {
            FileInputStream(descriptor.fileDescriptor).use { inputStream ->
                while (running.get()) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead < 0) {
                        break
                    }
                    val metadata = TunPacketMetadataParser.parse(buffer, bytesRead)
                    if (metadata == null) {
                        sink.onMalformedPacket(bytesRead)
                    } else {
                        sink.onPacket(metadata)
                    }
                }
            }
        } catch (_: IOException) {
            if (running.get()) {
                sink.onMalformedPacket(0)
            }
        } finally {
            running.set(false)
            sink.onCaptureStopped(clock())
        }
    }

    private companion object {
        const val MAX_PACKET_BYTES = 32_767
    }
}
