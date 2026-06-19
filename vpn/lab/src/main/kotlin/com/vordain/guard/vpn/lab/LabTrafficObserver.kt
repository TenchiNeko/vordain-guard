package com.vordain.guard.vpn.lab

interface LabTrafficObserver {
    fun reset(startedAtMillis: Long)
    fun observePacket(packet: ByteArray, length: Int, observedAtMillis: Long): LabTrafficObservationStats
    fun handlePacket(packet: ByteArray, length: Int, observedAtMillis: Long): LabPacketHandlingResult {
        return LabPacketHandlingResult(
            action = LabPacketAction.DROP,
            responseBytes = null,
            stats = observePacket(packet, length, observedAtMillis),
            decisionSummary = "Packet dropped; active lab response handling is not implemented by this observer",
        )
    }
    fun markDnsResponseWriteSuccess(observedAtMillis: Long): LabTrafficObservationStats = snapshot()
    fun markDnsResponseWriteFailure(observedAtMillis: Long): LabTrafficObservationStats = snapshot()
    fun markStopped(stoppedAtMillis: Long): LabTrafficObservationStats
    fun snapshot(): LabTrafficObservationStats
}
