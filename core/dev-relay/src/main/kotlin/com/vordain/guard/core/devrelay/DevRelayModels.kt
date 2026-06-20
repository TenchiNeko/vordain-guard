package com.vordain.guard.core.devrelay

import com.vordain.guard.core.model.DeviceId

enum class DevRelayDirection {
    PARENT_TO_CHILD,
    CHILD_TO_PARENT,
}

enum class DevRelayMessageStatus {
    PENDING,
    FETCHED,
    ACKNOWLEDGED,
}

data class DevRelayMessage(
    val messageId: String,
    val direction: DevRelayDirection,
    val sourceDeviceId: DeviceId,
    val targetDeviceId: DeviceId,
    val createdAtMillis: Long,
    val bundleText: String,
    val status: DevRelayMessageStatus = DevRelayMessageStatus.PENDING,
)

data class DevRelayInboxQuery(
    val targetDeviceId: DeviceId,
    val direction: DevRelayDirection? = null,
)

data class DevRelayMessageCodecResult(
    val accepted: Boolean,
    val reason: String,
    val message: DevRelayMessage? = null,
)

data class DevRelayMessagesCodecResult(
    val accepted: Boolean,
    val reason: String,
    val messages: List<DevRelayMessage> = emptyList(),
)

enum class DevRelayDebugCommandType {
    PARENT_RELAY_HEALTH_CHECK,
    PARENT_SEND_POLICY_BUNDLE,
    PARENT_FETCH_CHILD_STATUS_MESSAGES,
    PARENT_ACK_FETCHED_MESSAGE,
    PARENT_EXPORT_DEBUG_SNAPSHOT,
    CHILD_RELAY_HEALTH_CHECK,
    CHILD_FETCH_POLICY_BUNDLE,
    CHILD_IMPORT_LATEST_POLICY_BUNDLE,
    CHILD_SEND_STATUS_BUNDLE,
    CHILD_SEND_HEARTBEAT_STATUS_REPORT,
    CHILD_START_BASIC_DNS_GUARD,
    CHILD_STOP_BASIC_DNS_GUARD,
}

enum class DevRelayDebugCommandStatus {
    PENDING,
    FETCHED,
    COMPLETED,
}

data class DevRelayDebugCommand(
    val commandId: String,
    val type: DevRelayDebugCommandType,
    val sourceDeviceId: DeviceId,
    val targetDeviceId: DeviceId,
    val createdAtMillis: Long,
    val status: DevRelayDebugCommandStatus = DevRelayDebugCommandStatus.PENDING,
)

data class DevRelayDebugCommandResult(
    val resultId: String,
    val commandId: String,
    val type: DevRelayDebugCommandType,
    val sourceDeviceId: DeviceId,
    val targetDeviceId: DeviceId,
    val createdAtMillis: Long,
    val success: Boolean,
    val summary: String,
)

data class DevRelayDebugCommandCodecResult(
    val accepted: Boolean,
    val reason: String,
    val command: DevRelayDebugCommand? = null,
)

data class DevRelayDebugCommandsCodecResult(
    val accepted: Boolean,
    val reason: String,
    val commands: List<DevRelayDebugCommand> = emptyList(),
)

data class DevRelayDebugCommandResultCodecResult(
    val accepted: Boolean,
    val reason: String,
    val result: DevRelayDebugCommandResult? = null,
)

data class DevRelayDebugCommandResultsCodecResult(
    val accepted: Boolean,
    val reason: String,
    val results: List<DevRelayDebugCommandResult> = emptyList(),
)
