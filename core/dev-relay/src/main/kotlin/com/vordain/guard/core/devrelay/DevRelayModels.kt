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
