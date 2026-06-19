package com.vordain.guard.vpn.lab

interface LabTrafficObserver {
    fun reset(startedAtMillis: Long)
    fun observePacket(packet: ByteArray, length: Int, observedAtMillis: Long): LabTrafficObservationStats
    fun markStopped(stoppedAtMillis: Long): LabTrafficObservationStats
    fun snapshot(): LabTrafficObservationStats
}
