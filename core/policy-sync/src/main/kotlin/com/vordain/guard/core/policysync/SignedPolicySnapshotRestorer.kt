package com.vordain.guard.core.policysync

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.policy.Policy

data class PersistedSignedPolicySnapshot(
    val encodedPayload: String,
    val appliedAtMillis: Long,
    val expectedDeviceId: DeviceId,
    val lastKnownPolicyVersion: PolicyVersion?,
)

data class RestoredPolicyEvaluation(
    val accepted: Boolean,
    val policy: Policy?,
    val policyVersion: PolicyVersion?,
    val reason: PolicyUpdateVerificationResult,
    val decodedUpdateId: String?,
)

class SignedPolicySnapshotRestorer(
    private val codec: DebugPolicyUpdateCodec = DebugPolicyUpdateCodec(),
    private val applier: PolicyUpdateApplier = PolicyUpdateApplier(StubPolicyUpdateVerifier()),
) {
    fun restore(
        snapshot: PersistedSignedPolicySnapshot,
        currentTimeMillis: Long,
    ): RestoredPolicyEvaluation {
        val decoded = codec.decode(snapshot.encodedPayload)
        if (decoded !is DebugPolicyUpdateCodecResult.Decoded) {
            return RestoredPolicyEvaluation(
                accepted = false,
                policy = null,
                policyVersion = null,
                reason = PolicyUpdateVerificationResult.Malformed,
                decodedUpdateId = null,
            )
        }

        val applyResult = applier.apply(
            update = decoded.update,
            expectedDeviceId = snapshot.expectedDeviceId,
            currentTimeMillis = currentTimeMillis,
        )

        return RestoredPolicyEvaluation(
            accepted = applyResult.accepted,
            policy = applyResult.policy,
            policyVersion = applyResult.policyVersion,
            reason = applyResult.reason,
            decodedUpdateId = decoded.update.updateId,
        )
    }
}
