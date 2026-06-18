package com.vordain.guard.data.relay

sealed interface RelaySendResult {
    data object Success : RelaySendResult
    data class RetryableFailure(val reason: String) : RelaySendResult
    data class PermanentFailure(val reason: String) : RelaySendResult
}
