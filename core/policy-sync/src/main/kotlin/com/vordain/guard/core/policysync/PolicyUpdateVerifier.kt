package com.vordain.guard.core.policysync

import com.vordain.guard.core.model.DeviceId

interface PolicyUpdateVerifier {
    fun verify(
        update: SignedPolicyUpdate,
        expectedDeviceId: DeviceId,
        currentTimeMillis: Long,
    ): PolicyUpdateVerificationResult
}
