package com.vordain.guard.data.heartbeat

interface HeartbeatClient {
    fun send(heartbeat: ProtectionHeartbeat): HeartbeatSendResult
}
