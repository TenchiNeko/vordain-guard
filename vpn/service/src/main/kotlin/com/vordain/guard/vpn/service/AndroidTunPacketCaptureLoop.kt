package com.vordain.guard.vpn.service

import android.os.ParcelFileDescriptor
import com.vordain.guard.vpn.lab.LabPacketAction
import com.vordain.guard.vpn.lab.LabTrafficObserver
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class AndroidTunPacketCaptureLoop(
    private val descriptor: ParcelFileDescriptor,
    private val observer: LabTrafficObserver,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null

    fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }
        observer.reset(clock())
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
        observer.markStopped(clock())
    }

    private fun capturePackets() {
        val buffer = ByteArray(MAX_PACKET_BYTES)
        try {
            FileInputStream(descriptor.fileDescriptor).use { inputStream ->
                FileOutputStream(descriptor.fileDescriptor).use { outputStream ->
                    while (running.get()) {
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead < 0) {
                            break
                        }
                        val result = observer.handlePacket(buffer, bytesRead, clock())
                        if (
                            result.action == LabPacketAction.WRITE_DNS_BLOCK_RESPONSE ||
                            result.action == LabPacketAction.WRITE_DNS_UPSTREAM_RESPONSE
                        ) {
                            val responseBytes = result.responseBytes
                            if (responseBytes != null) {
                                try {
                                    outputStream.write(responseBytes)
                                    observer.markDnsResponseWriteSuccess(clock())
                                } catch (_: IOException) {
                                    observer.markDnsResponseWriteFailure(clock())
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: IOException) {
            if (running.get()) observer.observePacket(ByteArray(0), 0, clock())
        } finally {
            running.set(false)
            observer.markStopped(clock())
        }
    }

    private companion object {
        const val MAX_PACKET_BYTES = 32_767
    }
}
