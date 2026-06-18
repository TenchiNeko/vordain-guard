package com.vordain.guard.data.outbox

data class OutboxDispatchResult(
    val attemptedCount: Int,
    val deliveredCount: Int,
    val retryableFailureCount: Int,
    val permanentFailureCount: Int,
) {
    companion object {
        val Empty = OutboxDispatchResult(
            attemptedCount = 0,
            deliveredCount = 0,
            retryableFailureCount = 0,
            permanentFailureCount = 0,
        )
    }
}
