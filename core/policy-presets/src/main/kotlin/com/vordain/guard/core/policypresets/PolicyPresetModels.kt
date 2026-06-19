package com.vordain.guard.core.policypresets

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.policy.Policy

enum class PolicyPreset {
    BASIC_DNS_GUARD,
    STRICT_BROWSER,
    SCHOOL_FRIENDLY,
    HIGH_RISK_LOCKDOWN,
    CUSTOM,
}

data class PolicyPresetDefinition(
    val preset: PolicyPreset,
    val displayName: String,
    val description: String,
    val blockKnownProxyDomains: Boolean,
    val blockEncryptedDnsResolvers: Boolean,
    val blockUnknownDomains: Boolean,
    val lockdownMode: LockdownMode,
    val defaultAllowedDomains: Set<DomainName>,
    val defaultBlockedDomains: Set<DomainName>,
    val warningText: String,
)

class PolicyPresetFactory {
    fun describe(preset: PolicyPreset): PolicyPresetDefinition {
        return when (preset) {
            PolicyPreset.BASIC_DNS_GUARD -> PolicyPresetDefinition(
                preset = preset,
                displayName = "Basic DNS Guard",
                description = "Blocks normal DNS-based access to proxy and encrypted DNS resolver domains without blocking all unknown domains.",
                blockKnownProxyDomains = true,
                blockEncryptedDnsResolvers = true,
                blockUnknownDomains = false,
                lockdownMode = LockdownMode.STANDARD,
                defaultAllowedDomains = setOf(DomainName.from("school.edu"), DomainName.from("example.com")),
                defaultBlockedDomains = setOf(DomainName.from("blocked.example"), DomainName.from("proxy.example")),
                warningText = DEFAULT_WARNING,
            )
            PolicyPreset.STRICT_BROWSER -> PolicyPresetDefinition(
                preset = preset,
                displayName = "Strict Browser",
                description = "Stricter DNS lab policy for browser testing; unknown domains need parent review.",
                blockKnownProxyDomains = true,
                blockEncryptedDnsResolvers = true,
                blockUnknownDomains = true,
                lockdownMode = LockdownMode.STANDARD,
                defaultAllowedDomains = setOf(DomainName.from("school.edu"), DomainName.from("example.com")),
                defaultBlockedDomains = setOf(DomainName.from("blocked.example"), DomainName.from("proxy.example")),
                warningText = DEFAULT_WARNING,
            )
            PolicyPreset.SCHOOL_FRIENDLY -> PolicyPresetDefinition(
                preset = preset,
                displayName = "School Friendly",
                description = "Allows common school-safe sample domains while blocking proxy and encrypted DNS resolver bypass paths.",
                blockKnownProxyDomains = true,
                blockEncryptedDnsResolvers = true,
                blockUnknownDomains = false,
                lockdownMode = LockdownMode.STANDARD,
                defaultAllowedDomains = setOf(
                    DomainName.from("school.edu"),
                    DomainName.from("classroom.google.com"),
                    DomainName.from("khanacademy.org"),
                    DomainName.from("example.com"),
                ),
                defaultBlockedDomains = setOf(DomainName.from("blocked.example"), DomainName.from("proxy.example")),
                warningText = DEFAULT_WARNING,
            )
            PolicyPreset.HIGH_RISK_LOCKDOWN -> PolicyPresetDefinition(
                preset = preset,
                displayName = "High Risk Lockdown",
                description = "Strictest DNS lab preset for high-risk review; unknown domains are blocked until reviewed.",
                blockKnownProxyDomains = true,
                blockEncryptedDnsResolvers = true,
                blockUnknownDomains = true,
                lockdownMode = LockdownMode.STANDARD,
                defaultAllowedDomains = setOf(DomainName.from("school.edu")),
                defaultBlockedDomains = setOf(
                    DomainName.from("blocked.example"),
                    DomainName.from("proxy.example"),
                    DomainName.from("vpn.example"),
                ),
                warningText = "$DEFAULT_WARNING High Risk Lockdown is a stricter lab preset, not a production safety guarantee.",
            )
            PolicyPreset.CUSTOM -> PolicyPresetDefinition(
                preset = preset,
                displayName = "Custom",
                description = "Custom DNS-only lab policy based on the Basic DNS Guard baseline.",
                blockKnownProxyDomains = true,
                blockEncryptedDnsResolvers = true,
                blockUnknownDomains = false,
                lockdownMode = LockdownMode.STANDARD,
                defaultAllowedDomains = emptySet(),
                defaultBlockedDomains = setOf(DomainName.from("blocked.example")),
                warningText = DEFAULT_WARNING,
            )
        }
    }

    fun createPolicy(
        preset: PolicyPreset,
        policyId: PolicyId = PolicyId("preset-${preset.name.lowercase().replace('_', '-')}"),
        allowedDomainsOverride: Set<DomainName>? = null,
        blockedDomainsOverride: Set<DomainName>? = null,
    ): Policy {
        val definition = describe(preset)
        return Policy(
            id = policyId,
            mode = definition.lockdownMode,
            allowedDomains = allowedDomainsOverride ?: definition.defaultAllowedDomains,
            blockedDomains = blockedDomainsOverride ?: definition.defaultBlockedDomains,
            allowedPackages = emptySet<AppPackageName>(),
            blockedPackages = emptySet<AppPackageName>(),
            blockUnknownDomains = definition.blockUnknownDomains,
            blockKnownProxyDomains = definition.blockKnownProxyDomains,
        )
    }

    companion object {
        const val DEFAULT_WARNING = "DNS-only filtering is not production-enabled yet. Not full protection."
    }
}
