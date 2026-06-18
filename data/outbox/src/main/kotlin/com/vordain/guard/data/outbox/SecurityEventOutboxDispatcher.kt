package com.vordain.guard.data.outbox

import com.vordain.guard.data.local.SecurityEventQueue
import com.vordain.guard.data.relay.RelayClient
import com.vordain.guard.data.relay.RelayMessage
import com.vordain.guard.data.relay.RelaySendResult

class SecurityEventOutboxDispatcher(
    private val securityEventQueue: SecurityEventQueue,
    private val payloadEncryptor: SecurityEventPayloadEncryptor,
    private val relayClient: RelayClient,
    private val relayMessageIdProvider: RelayMessageIdProvider,
    private val outboxClock: OutboxClock,
) {
    fun dispatch(request: OutboxDispatchRequest): OutboxDispatchResult {
        if (request.limit <= 0) {
            return OutboxDispatchResult.Empty
        }

        var attemptedCount = 0
        var deliveredCount = 0
        var retryableFailureCount = 0
        var permanentFailureCount = 0

        securityEventQueue.pending(request.limit).forEach { queuedEvent ->
            attemptedCount += 1
            val event = queuedEvent.event
            val encryptedPayload = try {
                payloadEncryptor.encrypt(event, request.targetDeviceId)
            } catch (_: RuntimeException) {
                securityEventQueue.markFailed(event.id)
                retryableFailureCount += 1
                return@forEach
            }

            val relayMessage = RelayMessage(
                messageId = relayMessageIdProvider.nextId(),
                sourceDeviceId = request.sourceDeviceId,
                targetDeviceId = request.targetDeviceId,
                payload = encryptedPayload,
                createdAtMillis = outboxClock.nowMillis(),
            )

            when (relayClient.send(relayMessage)) {
                RelaySendResult.Success -> {
                    securityEventQueue.markDelivered(event.id)
                    deliveredCount += 1
                }
                is RelaySendResult.RetryableFailure -> {
                    securityEventQueue.markFailed(event.id)
                    retryableFailureCount += 1
                }
                is RelaySendResult.PermanentFailure -> {
                    securityEventQueue.markFailed(event.id)
                    permanentFailureCount += 1
                }
            }
        }

        return OutboxDispatchResult(
            attemptedCount = attemptedCount,
            deliveredCount = deliveredCount,
            retryableFailureCount = retryableFailureCount,
            permanentFailureCount = permanentFailureCount,
        )
    }
}
