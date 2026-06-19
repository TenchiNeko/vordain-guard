package com.vordain.guard.vpn.service

import android.net.VpnService
import com.vordain.guard.vpn.lab.LabDnsUpstreamQuery
import com.vordain.guard.vpn.lab.LabDnsUpstreamResult
import com.vordain.guard.vpn.lab.LabDnsUpstreamTransport
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class AndroidProtectedUdpDnsTransport(
    private val vpnService: VpnService,
    private val defaultUpstreamHost: String = "1.1.1.1",
    private val defaultUpstreamPort: Int = 53,
    private val timeoutMillis: Int = 1_500,
) : LabDnsUpstreamTransport {
    override fun query(query: LabDnsUpstreamQuery): LabDnsUpstreamResult {
        val host = query.upstreamHost.ifBlank { defaultUpstreamHost }
        val port = if (query.upstreamPort > 0) query.upstreamPort else defaultUpstreamPort
        val socket = DatagramSocket()
        return try {
            if (!vpnService.protect(socket)) {
                return LabDnsUpstreamResult.failure("VPN service could not protect DNS upstream socket")
            }
            socket.soTimeout = timeoutMillis
            val address = InetAddress.getByName(host)
            val request = DatagramPacket(query.dnsPayload, query.dnsPayload.size, address, port)
            socket.send(request)

            val responseBuffer = ByteArray(MAX_DNS_RESPONSE_BYTES)
            val response = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(response)
            LabDnsUpstreamResult.success(response.data.copyOf(response.length))
        } catch (_: SocketTimeoutException) {
            LabDnsUpstreamResult.failure("timeout waiting for protected DNS upstream response")
        } catch (error: IOException) {
            LabDnsUpstreamResult.failure(error.message ?: "protected DNS upstream I/O failure")
        } catch (error: SecurityException) {
            LabDnsUpstreamResult.failure(error.message ?: "protected DNS upstream permission failure")
        } finally {
            socket.close()
        }
    }

    private companion object {
        const val MAX_DNS_RESPONSE_BYTES = 4_096
    }
}
