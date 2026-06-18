package com.vordain.guard.vpn.dns

interface DnsObserver {
    fun observe(packetBytes: ByteArray): DnsObservation?
}
