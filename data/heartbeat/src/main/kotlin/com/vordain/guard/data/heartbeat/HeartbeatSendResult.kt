package com.vordain.guard.data.heartbeat

sealed interface HeartbeatSendResult {
    data object Success : HeartbeatSendResult
    data class RetryableFailure(val reason: String) : HeartbeatSendResult
    data class PermanentFailure(val reason: String) : HeartbeatSendResult
}
