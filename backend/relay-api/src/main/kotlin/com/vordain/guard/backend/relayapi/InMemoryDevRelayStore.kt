package com.vordain.guard.backend.relayapi

import com.vordain.guard.core.devrelay.DevRelayDirection
import com.vordain.guard.core.devrelay.DevRelayInboxQuery
import com.vordain.guard.core.devrelay.DevRelayMessage
import com.vordain.guard.core.devrelay.DevRelayMessageStatus

class InMemoryDevRelayStore {
    private val messages = linkedMapOf<String, DevRelayMessage>()

    @Synchronized
    fun put(message: DevRelayMessage): DevRelayMessage {
        messages[message.messageId] = message
        return message
    }

    @Synchronized
    fun query(query: DevRelayInboxQuery): List<DevRelayMessage> {
        val matches = messages.values.filter { message ->
            message.targetDeviceId == query.targetDeviceId &&
                message.status != DevRelayMessageStatus.ACKNOWLEDGED &&
                (query.direction == null || message.direction == query.direction)
        }
        matches.forEach { message ->
            if (message.status == DevRelayMessageStatus.PENDING) {
                messages[message.messageId] = message.copy(status = DevRelayMessageStatus.FETCHED)
            }
        }
        return matches.map { message ->
            if (message.status == DevRelayMessageStatus.PENDING) {
                message.copy(status = DevRelayMessageStatus.FETCHED)
            } else {
                message
            }
        }
    }

    @Synchronized
    fun acknowledge(messageId: String): Boolean {
        val existing = messages[messageId] ?: return false
        messages[messageId] = existing.copy(status = DevRelayMessageStatus.ACKNOWLEDGED)
        return true
    }

    @Synchronized
    fun stats(): DevRelayStoreStats {
        return DevRelayStoreStats(
            totalCount = messages.size,
            pendingCount = messages.values.count { it.status == DevRelayMessageStatus.PENDING },
            fetchedCount = messages.values.count { it.status == DevRelayMessageStatus.FETCHED },
            acknowledgedCount = messages.values.count { it.status == DevRelayMessageStatus.ACKNOWLEDGED },
        )
    }
}

data class DevRelayStoreStats(
    val totalCount: Int,
    val pendingCount: Int,
    val fetchedCount: Int,
    val acknowledgedCount: Int,
)
