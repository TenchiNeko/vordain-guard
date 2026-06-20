package com.vordain.guard.backend.relayapi

import com.vordain.guard.core.devrelay.DevRelayDirection
import com.vordain.guard.core.devrelay.DevRelayDebugCommand
import com.vordain.guard.core.devrelay.DevRelayDebugCommandResult
import com.vordain.guard.core.devrelay.DevRelayDebugCommandStatus
import com.vordain.guard.core.devrelay.DevRelayInboxQuery
import com.vordain.guard.core.devrelay.DevRelayMessage
import com.vordain.guard.core.devrelay.DevRelayMessageStatus
import com.vordain.guard.core.model.DeviceId

class InMemoryDevRelayStore {
    private val messages = linkedMapOf<String, DevRelayMessage>()
    private val debugCommands = linkedMapOf<String, DevRelayDebugCommand>()
    private val debugResults = linkedMapOf<String, DevRelayDebugCommandResult>()

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
            pendingDebugCommandCount = debugCommands.values.count { it.status == DevRelayDebugCommandStatus.PENDING },
            debugResultCount = debugResults.size,
        )
    }

    @Synchronized
    fun putDebugCommand(command: DevRelayDebugCommand): DevRelayDebugCommand {
        debugCommands[command.commandId] = command
        return command
    }

    @Synchronized
    fun queryDebugCommands(targetDeviceId: DeviceId): List<DevRelayDebugCommand> {
        val matches = debugCommands.values.filter { command ->
            command.targetDeviceId == targetDeviceId &&
                command.status == DevRelayDebugCommandStatus.PENDING
        }
        matches.forEach { command ->
            debugCommands[command.commandId] = command.copy(status = DevRelayDebugCommandStatus.FETCHED)
        }
        return matches.map { command ->
            command.copy(status = DevRelayDebugCommandStatus.FETCHED)
        }
    }

    @Synchronized
    fun putDebugResult(result: DevRelayDebugCommandResult): DevRelayDebugCommandResult {
        debugResults[result.resultId] = result
        debugCommands[result.commandId]?.let { command ->
            debugCommands[result.commandId] = command.copy(status = DevRelayDebugCommandStatus.COMPLETED)
        }
        return result
    }

    @Synchronized
    fun queryDebugResults(targetDeviceId: DeviceId): List<DevRelayDebugCommandResult> {
        return debugResults.values.filter { result ->
            result.targetDeviceId == targetDeviceId
        }
    }
}

data class DevRelayStoreStats(
    val totalCount: Int,
    val pendingCount: Int,
    val fetchedCount: Int,
    val acknowledgedCount: Int,
    val pendingDebugCommandCount: Int = 0,
    val debugResultCount: Int = 0,
)
