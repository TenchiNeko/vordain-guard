package com.vordain.guard.core.pairing

import com.vordain.guard.core.model.DeviceId

enum class PairingRole {
    PARENT,
    CHILD,
}

@JvmInline
value class PairingSessionId(val value: String)

@JvmInline
value class PairingPublicKeyFingerprint(val value: String) {
    init {
        require(value.isNotBlank()) { "Pairing fingerprint must not be blank" }
    }
}

@JvmInline
value class PairingVerificationCode(val value: String) {
    init {
        require(value.isNotBlank()) { "Pairing verification code must not be blank" }
    }
}

enum class PairingCapability {
    POLICY_UPDATES,
    HEARTBEAT_STATUS,
    ENCRYPTED_ALERTS,
    PARENT_REVIEW,
    MANAGED_LOCKDOWN_READY,
}

data class PairingDeviceProfile(
    val deviceId: DeviceId,
    val role: PairingRole,
    val displayName: String,
    val publicKeyFingerprint: PairingPublicKeyFingerprint,
    val capabilities: Set<PairingCapability>,
) {
    init {
        require(displayName.isNotBlank()) { "Pairing display name must not be blank" }
    }
}

data class PairingInvite(
    val sessionId: PairingSessionId,
    val parentDeviceProfile: PairingDeviceProfile,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val verificationCode: PairingVerificationCode,
)

data class PairingAcceptance(
    val sessionId: PairingSessionId,
    val childDeviceProfile: PairingDeviceProfile,
    val acceptedAtMillis: Long,
    val verificationCode: PairingVerificationCode,
)

enum class PairingStatus {
    NOT_PAIRED,
    INVITE_CREATED,
    ACCEPTED_BY_CHILD,
    PAIRED,
    EXPIRED,
    REJECTED,
    MALFORMED,
}

data class PairingEvaluationResult(
    val status: PairingStatus,
    val reason: String,
    val sessionId: PairingSessionId?,
    val pairedParent: PairingDeviceProfile?,
    val pairedChild: PairingDeviceProfile?,
)

class PairingEvaluator {
    fun evaluateInvite(
        invite: PairingInvite,
        currentTimeMillis: Long,
    ): PairingEvaluationResult {
        return when {
            invite.expiresAtMillis <= invite.createdAtMillis -> result(
                status = PairingStatus.MALFORMED,
                reason = "Invite expiry must be after creation time",
                invite = invite,
            )
            currentTimeMillis >= invite.expiresAtMillis -> result(
                status = PairingStatus.EXPIRED,
                reason = "Invite expired",
                invite = invite,
            )
            else -> result(
                status = PairingStatus.INVITE_CREATED,
                reason = "Invite is usable",
                invite = invite,
            )
        }
    }

    fun evaluateAcceptance(
        invite: PairingInvite,
        acceptance: PairingAcceptance,
        currentTimeMillis: Long,
    ): PairingEvaluationResult {
        val inviteResult = evaluateInvite(invite, currentTimeMillis)
        if (inviteResult.status != PairingStatus.INVITE_CREATED) {
            return inviteResult
        }

        return when {
            acceptance.sessionId != invite.sessionId -> result(
                status = PairingStatus.REJECTED,
                reason = "Pairing session does not match",
                invite = invite,
                acceptance = acceptance,
            )
            acceptance.verificationCode != invite.verificationCode -> result(
                status = PairingStatus.REJECTED,
                reason = "Pairing verification code does not match",
                invite = invite,
                acceptance = acceptance,
            )
            acceptance.acceptedAtMillis >= invite.expiresAtMillis -> result(
                status = PairingStatus.EXPIRED,
                reason = "Acceptance arrived after invite expiry",
                invite = invite,
                acceptance = acceptance,
            )
            else -> result(
                status = PairingStatus.PAIRED,
                reason = "Pairing accepted",
                invite = invite,
                acceptance = acceptance,
            )
        }
    }

    private fun result(
        status: PairingStatus,
        reason: String,
        invite: PairingInvite,
        acceptance: PairingAcceptance? = null,
    ): PairingEvaluationResult {
        return PairingEvaluationResult(
            status = status,
            reason = reason,
            sessionId = invite.sessionId,
            pairedParent = if (status == PairingStatus.PAIRED) invite.parentDeviceProfile else null,
            pairedChild = if (status == PairingStatus.PAIRED) acceptance?.childDeviceProfile else null,
        )
    }
}
