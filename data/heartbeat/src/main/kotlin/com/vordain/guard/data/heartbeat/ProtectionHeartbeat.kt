package com.vordain.guard.data.heartbeat

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.model.ProtectionState

data class ProtectionHeartbeat(
    val deviceId: DeviceId,
    val reportedAtMillis: Long,
    val sequenceNumber: Long,
    val protectionState: ProtectionState,
    val policyId: PolicyId?,
)
