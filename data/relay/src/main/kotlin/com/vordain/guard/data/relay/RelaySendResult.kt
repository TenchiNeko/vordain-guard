package com.vordain.guard.data.relay

sealed interface RelaySendResult {
    data object Accepted : RelaySendResult
    data class Rejected(val reason: String) : RelaySendResult
    data class RetryLater(val reason: String) : RelaySendResult
}
