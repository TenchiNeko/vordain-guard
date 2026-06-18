package com.vordain.guard.core.status

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.PolicyId

data class ProtectionReport(
    val deviceId: DeviceId,
    val reportedAtMillis: Long,
    val vpnActive: Boolean,
    val policyLoaded: Boolean,
    val policyVersion: PolicyId?,
    val alwaysOnVpnEnabled: Boolean,
    val blockWithoutVpnEnabled: Boolean,
    val appProtectionEnabled: Boolean,
    val localTamperDetected: Boolean,
)
