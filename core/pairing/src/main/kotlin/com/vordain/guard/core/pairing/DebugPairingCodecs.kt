package com.vordain.guard.core.pairing

import com.vordain.guard.core.model.DeviceId

class DebugPairingInviteCodec {
    fun encode(invite: PairingInvite): String {
        return listOf(
            INVITE_HEADER,
            "sessionId=${invite.sessionId.value}",
            "parentDeviceId=${invite.parentDeviceProfile.deviceId.value}",
            "parentDisplayName=${invite.parentDeviceProfile.displayName}",
            "parentFingerprint=${invite.parentDeviceProfile.publicKeyFingerprint.value}",
            "verificationCode=${invite.verificationCode.value}",
            "createdAtMillis=${invite.createdAtMillis}",
            "expiresAtMillis=${invite.expiresAtMillis}",
            "capabilities=${invite.parentDeviceProfile.capabilities.toPayloadValue()}",
        ).joinToString(separator = "\n")
    }

    fun decode(payload: String): DebugPairingInviteCodecResult {
        return runCatching {
            val fields = parsePayload(payload = payload, expectedHeader = INVITE_HEADER)
            PairingInvite(
                sessionId = PairingSessionId(fields.required("sessionId")),
                parentDeviceProfile = PairingDeviceProfile(
                    deviceId = DeviceId(fields.required("parentDeviceId")),
                    role = PairingRole.PARENT,
                    displayName = fields.required("parentDisplayName"),
                    publicKeyFingerprint = PairingPublicKeyFingerprint(fields.required("parentFingerprint")),
                    capabilities = fields.required("capabilities").toCapabilities(),
                ),
                verificationCode = PairingVerificationCode(fields.required("verificationCode")),
                createdAtMillis = fields.required("createdAtMillis").toLongStrict("createdAtMillis"),
                expiresAtMillis = fields.required("expiresAtMillis").toLongStrict("expiresAtMillis"),
            )
        }.fold(
            onSuccess = DebugPairingInviteCodecResult::Decoded,
            onFailure = { throwable ->
                DebugPairingInviteCodecResult.Rejected(throwable.message ?: "Debug pairing invite could not be decoded")
            },
        )
    }
}

class DebugPairingAcceptanceCodec {
    fun encode(acceptance: PairingAcceptance): String {
        return listOf(
            ACCEPTANCE_HEADER,
            "sessionId=${acceptance.sessionId.value}",
            "childDeviceId=${acceptance.childDeviceProfile.deviceId.value}",
            "childDisplayName=${acceptance.childDeviceProfile.displayName}",
            "childFingerprint=${acceptance.childDeviceProfile.publicKeyFingerprint.value}",
            "verificationCode=${acceptance.verificationCode.value}",
            "acceptedAtMillis=${acceptance.acceptedAtMillis}",
            "capabilities=${acceptance.childDeviceProfile.capabilities.toPayloadValue()}",
        ).joinToString(separator = "\n")
    }

    fun decode(payload: String): DebugPairingAcceptanceCodecResult {
        return runCatching {
            val fields = parsePayload(payload = payload, expectedHeader = ACCEPTANCE_HEADER)
            PairingAcceptance(
                sessionId = PairingSessionId(fields.required("sessionId")),
                childDeviceProfile = PairingDeviceProfile(
                    deviceId = DeviceId(fields.required("childDeviceId")),
                    role = PairingRole.CHILD,
                    displayName = fields.required("childDisplayName"),
                    publicKeyFingerprint = PairingPublicKeyFingerprint(fields.required("childFingerprint")),
                    capabilities = fields.required("capabilities").toCapabilities(),
                ),
                verificationCode = PairingVerificationCode(fields.required("verificationCode")),
                acceptedAtMillis = fields.required("acceptedAtMillis").toLongStrict("acceptedAtMillis"),
            )
        }.fold(
            onSuccess = DebugPairingAcceptanceCodecResult::Decoded,
            onFailure = { throwable ->
                DebugPairingAcceptanceCodecResult.Rejected(throwable.message ?: "Debug pairing acceptance could not be decoded")
            },
        )
    }
}

sealed class DebugPairingInviteCodecResult {
    data class Decoded(val invite: PairingInvite) : DebugPairingInviteCodecResult()
    data class Rejected(val reason: String) : DebugPairingInviteCodecResult()
}

sealed class DebugPairingAcceptanceCodecResult {
    data class Decoded(val acceptance: PairingAcceptance) : DebugPairingAcceptanceCodecResult()
    data class Rejected(val reason: String) : DebugPairingAcceptanceCodecResult()
}

private const val INVITE_HEADER = "VORDAIN_DEBUG_PAIRING_INVITE_V1"
private const val ACCEPTANCE_HEADER = "VORDAIN_DEBUG_PAIRING_ACCEPTANCE_V1"

private fun parsePayload(
    payload: String,
    expectedHeader: String,
): Map<String, String> {
    val lines = payload.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toList()

    require(lines.firstOrNull() == expectedHeader) { "Unsupported debug pairing header" }

    return lines.drop(1).associate { line ->
        val separatorIndex = line.indexOf('=')
        require(separatorIndex > 0) { "Malformed field: $line" }
        line.substring(0, separatorIndex).trim() to line.substring(separatorIndex + 1).trim()
    }
}

private fun Map<String, String>.required(key: String): String {
    val value = this[key].orEmpty().trim()
    require(value.isNotBlank()) { "$key is required" }
    return value
}

private fun String.toLongStrict(fieldName: String): Long {
    return toLongOrNull() ?: throw IllegalArgumentException("$fieldName must be a number")
}

private fun String.toCapabilities(): Set<PairingCapability> {
    return split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map(PairingCapability::valueOf)
        .toSet()
}

private fun Set<PairingCapability>.toPayloadValue(): String {
    return map(PairingCapability::name).sorted().joinToString(separator = ",")
}
