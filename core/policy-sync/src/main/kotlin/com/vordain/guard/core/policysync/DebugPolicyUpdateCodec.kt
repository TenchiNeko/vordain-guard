package com.vordain.guard.core.policysync

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.policy.Policy

class DebugPolicyUpdateCodec {
    fun encode(update: SignedPolicyUpdate): String {
        return listOf(
            HEADER,
            "targetDeviceId=${update.targetDeviceId.value}",
            "policyVersion=${update.policyVersion.value}",
            "issuedAtMillis=${update.issuedAtMillis}",
            "expiresAtMillis=${update.expiresAtMillis}",
            "signature=${update.signature.value}",
            "presetName=${update.presetName.orEmpty()}",
            "policyDisplayLabel=${update.policyDisplayLabel.orEmpty()}",
            "blockEncryptedDnsResolvers=${update.blockEncryptedDnsResolvers}",
            "lockdownMode=${update.policy.mode.name}",
            "blockUnknownDomains=${update.policy.blockUnknownDomains}",
            "blockKnownProxyDomains=${update.policy.blockKnownProxyDomains}",
            "allowDomains=${update.policy.allowedDomains.toPayloadValue()}",
            "blockDomains=${update.policy.blockedDomains.toPayloadValue()}",
        ).joinToString(separator = "\n")
    }

    fun decode(payload: String): DebugPolicyUpdateCodecResult {
        return runCatching {
            val lines = payload.lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toList()

            require(lines.firstOrNull() == HEADER) { "Unsupported debug policy update header" }

            val fields = lines.drop(1).map { line ->
                val separatorIndex = line.indexOf('=')
                require(separatorIndex > 0) { "Malformed field: $line" }
                line.substring(0, separatorIndex) to line.substring(separatorIndex + 1)
            }.toMap()

            val targetDeviceId = fields.required("targetDeviceId")
            val policyVersion = fields.required("policyVersion")
            val issuedAtMillis = fields.required("issuedAtMillis").toLongOrNull()
                ?: throw IllegalArgumentException("issuedAtMillis must be a number")
            val expiresAtMillis = fields.required("expiresAtMillis").toLongOrNull()
                ?: throw IllegalArgumentException("expiresAtMillis must be a number")
            require(expiresAtMillis > issuedAtMillis) { "expiresAtMillis must be greater than issuedAtMillis" }

            val signature = fields.required("signature")
            require(signature.isNotBlank()) { "signature must not be blank" }
            val presetName = fields["presetName"]
                ?.takeIf(String::isNotBlank)
                ?.let(::normalizePresetName)
            val policyDisplayLabel = fields["policyDisplayLabel"]?.takeIf(String::isNotBlank)

            val lockdownMode = fields["lockdownMode"]
                ?.takeIf(String::isNotBlank)
                ?.let(LockdownMode::valueOf)
                ?: LockdownMode.STANDARD

            val update = SignedPolicyUpdate(
                updateId = "debug-$policyVersion",
                targetDeviceId = DeviceId(targetDeviceId),
                policyVersion = PolicyVersion(policyVersion),
                issuedAtMillis = issuedAtMillis,
                expiresAtMillis = expiresAtMillis,
                signature = PolicyUpdateSignature(signature),
                presetName = presetName,
                blockEncryptedDnsResolvers = fields["blockEncryptedDnsResolvers"].toBooleanOrDefault(defaultValue = true),
                policyDisplayLabel = policyDisplayLabel,
                policy = Policy(
                    id = PolicyId("debug-$policyVersion"),
                    mode = lockdownMode,
                    allowedDomains = fields["allowDomains"].toDomainSet(),
                    blockedDomains = fields["blockDomains"].toDomainSet(),
                    allowedPackages = emptySet(),
                    blockedPackages = emptySet(),
                    blockUnknownDomains = fields["blockUnknownDomains"].toBooleanOrDefault(defaultValue = false),
                    blockKnownProxyDomains = fields["blockKnownProxyDomains"].toBooleanOrDefault(defaultValue = true),
                ),
            )

            DebugPolicyUpdateCodecResult.Decoded(update)
        }.getOrElse { throwable ->
            DebugPolicyUpdateCodecResult.Rejected(throwable.message ?: "Debug policy update could not be decoded")
        }
    }

    private fun Map<String, String>.required(key: String): String {
        val value = this[key]?.trim().orEmpty()
        require(value.isNotBlank()) { "$key is required" }
        return value
    }

    private fun Set<DomainName>.toPayloadValue(): String {
        return map(DomainName::value).sorted().joinToString(separator = ",")
    }

    private fun String?.toDomainSet(): Set<DomainName> {
        return orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(DomainName::from)
            .toSet()
    }

    private fun String?.toBooleanOrDefault(defaultValue: Boolean): Boolean {
        return when (this?.trim()?.lowercase()) {
            "true" -> true
            "false" -> false
            null, "" -> defaultValue
            else -> throw IllegalArgumentException("Boolean field must be true or false")
        }
    }

    private fun normalizePresetName(rawValue: String): String {
        val value = rawValue.trim()
        return when (value) {
            "BASIC_DNS_GUARD",
            "STRICT_BROWSER",
            "SCHOOL_FRIENDLY",
            "HIGH_RISK_LOCKDOWN",
            "CUSTOM",
            -> value
            else -> "CUSTOM"
        }
    }

    private companion object {
        const val HEADER = "VORDAIN_DEBUG_POLICY_UPDATE_V1"
    }
}

sealed class DebugPolicyUpdateCodecResult {
    data class Decoded(val update: SignedPolicyUpdate) : DebugPolicyUpdateCodecResult()
    data class Rejected(val reason: String) : DebugPolicyUpdateCodecResult()
}
