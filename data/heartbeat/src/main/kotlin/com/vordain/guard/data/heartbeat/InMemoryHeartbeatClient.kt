package com.vordain.guard.data.heartbeat

class InMemoryHeartbeatClient(
    private val sendResult: HeartbeatSendResult = HeartbeatSendResult.Success,
) : HeartbeatClient {
    private val sentHeartbeats = mutableListOf<ProtectionHeartbeat>()

    override fun send(heartbeat: ProtectionHeartbeat): HeartbeatSendResult {
        if (sendResult == HeartbeatSendResult.Success) {
            sentHeartbeats += heartbeat.copy()
        }
        return sendResult
    }

    fun sent(): List<ProtectionHeartbeat> {
        return sentHeartbeats.map { heartbeat -> heartbeat.copy() }
    }
}
