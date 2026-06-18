package com.vordain.guard.core.policysync

import com.vordain.guard.core.model.DeviceId

class StubPolicyUpdateVerifier : PolicyUpdateVerifier {
    override fun verify(
        update: SignedPolicyUpdate,
        expectedDeviceId: DeviceId,
        currentTimeMillis: Long,
    ): PolicyUpdateVerificationResult {
        return when {
            update.expiresAtMillis <= update.issuedAtMillis -> PolicyUpdateVerificationResult.Malformed
            currentTimeMillis < update.issuedAtMillis -> PolicyUpdateVerificationResult.NotYetValid
            currentTimeMillis >= update.expiresAtMillis -> PolicyUpdateVerificationResult.Expired
            update.targetDeviceId != expectedDeviceId -> PolicyUpdateVerificationResult.WrongDevice
            update.signature.value.isBlank() -> PolicyUpdateVerificationResult.InvalidSignature
            else -> PolicyUpdateVerificationResult.Valid
        }
    }
}
