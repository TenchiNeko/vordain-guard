package com.vordain.guard.child

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.policysync.DebugPolicyUpdateCodec
import com.vordain.guard.core.policysync.DebugPolicyUpdateCodecResult
import com.vordain.guard.core.policysync.PolicyUpdateApplier
import com.vordain.guard.core.policysync.PolicyUpdateVerificationResult
import com.vordain.guard.core.policysync.PolicyVersion
import com.vordain.guard.core.policysync.StubPolicyUpdateVerifier

class ChildDebugPolicyHandoff(
    private val codec: DebugPolicyUpdateCodec = DebugPolicyUpdateCodec(),
    private val applier: PolicyUpdateApplier = PolicyUpdateApplier(StubPolicyUpdateVerifier()),
    private val currentTimeMillisProvider: () -> Long,
) {
    fun apply(
        expectedDeviceId: DeviceId,
        payload: String,
    ): ChildDebugPolicyHandoffResult {
        return when (val decoded = codec.decode(payload)) {
            is DebugPolicyUpdateCodecResult.Rejected -> ChildDebugPolicyHandoffResult(
                accepted = false,
                reason = "DecodeRejected",
                policyVersion = null,
                allowDomainCount = 0,
                blockDomainCount = 0,
                presetName = null,
                policyDisplayLabel = null,
                blockEncryptedDnsResolvers = true,
                policy = null,
                message = decoded.reason,
            )
            is DebugPolicyUpdateCodecResult.Decoded -> {
                val applyResult = applier.apply(
                    update = decoded.update,
                    expectedDeviceId = expectedDeviceId,
                    currentTimeMillis = currentTimeMillisProvider(),
                )
                ChildDebugPolicyHandoffResult(
                    accepted = applyResult.accepted,
                    reason = applyResult.reason.name,
                    policyVersion = applyResult.policyVersion,
                    allowDomainCount = applyResult.policy?.allowedDomains?.size ?: 0,
                    blockDomainCount = applyResult.policy?.blockedDomains?.size ?: 0,
                    presetName = decoded.update.presetName,
                    policyDisplayLabel = decoded.update.policyDisplayLabel,
                    blockEncryptedDnsResolvers = decoded.update.blockEncryptedDnsResolvers,
                    policy = applyResult.policy,
                    message = if (applyResult.reason == PolicyUpdateVerificationResult.Valid) {
                        "Debug policy update accepted in memory"
                    } else {
                        "Debug policy update rejected"
                    },
                )
            }
        }
    }
}

data class ChildDebugPolicyHandoffResult(
    val accepted: Boolean,
    val reason: String,
    val policyVersion: PolicyVersion?,
    val allowDomainCount: Int,
    val blockDomainCount: Int,
    val presetName: String?,
    val policyDisplayLabel: String?,
    val blockEncryptedDnsResolvers: Boolean,
    val policy: com.vordain.guard.core.policy.Policy?,
    val message: String,
) {
    fun asDisplayText(): String {
        return listOf(
            "Accepted: ${if (accepted) "yes" else "no"}",
            "Reason: $reason",
            "Current debug policy version: ${policyVersion?.value ?: "unchanged"}",
            "Preset: ${policyDisplayLabel ?: presetName ?: "unspecified"}",
            "Allowed domains: $allowDomainCount",
            "Blocked domains: $blockDomainCount",
            "Encrypted DNS resolver blocking: ${if (blockEncryptedDnsResolvers) "enabled" else "not requested"}",
            message,
            "This build does not filter traffic yet. Policy tester only.",
        ).joinToString(separator = "\n")
    }
}
