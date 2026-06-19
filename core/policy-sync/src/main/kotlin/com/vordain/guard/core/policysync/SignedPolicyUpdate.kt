package com.vordain.guard.core.policysync

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.policy.Policy

data class SignedPolicyUpdate(
    val updateId: String,
    val targetDeviceId: DeviceId,
    val policy: Policy,
    val policyVersion: PolicyVersion,
    val issuedAtMillis: Long,
    val expiresAtMillis: Long,
    val signature: PolicyUpdateSignature,
    val presetName: String? = null,
    val blockEncryptedDnsResolvers: Boolean = true,
    val policyDisplayLabel: String? = null,
)
