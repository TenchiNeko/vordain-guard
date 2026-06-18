package com.vordain.guard.core.policysync

import com.vordain.guard.core.model.DeviceId

class PolicyUpdateApplier(
    private val policyUpdateVerifier: PolicyUpdateVerifier,
) {
    fun apply(
        update: SignedPolicyUpdate,
        expectedDeviceId: DeviceId,
        currentTimeMillis: Long,
    ): PolicyUpdateApplyResult {
        val verificationResult = policyUpdateVerifier.verify(
            update = update,
            expectedDeviceId = expectedDeviceId,
            currentTimeMillis = currentTimeMillis,
        )
        return if (verificationResult == PolicyUpdateVerificationResult.Valid) {
            PolicyUpdateApplyResult(
                accepted = true,
                policy = update.policy,
                policyVersion = update.policyVersion,
                reason = verificationResult,
            )
        } else {
            PolicyUpdateApplyResult(
                accepted = false,
                policy = null,
                policyVersion = null,
                reason = verificationResult,
            )
        }
    }
}
