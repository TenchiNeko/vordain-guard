package com.vordain.guard.core.intelligence

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName

class IntelligenceBundleEvaluator(
    private val bundle: IntelligenceBundle,
) {
    fun findRecord(domain: DomainName): DomainIntelligenceRecord? {
        return bundle.domainRecords.firstOrNull { record ->
            matches(candidate = domain, rule = record.domain)
        }
    }

    fun findProfile(appPackageName: AppPackageName): AppCompatibilityProfile? {
        return bundle.appProfiles.firstOrNull { profile ->
            profile.appPackageName == appPackageName
        }
    }

    fun isExpired(currentTimeMillis: Long): Boolean {
        return currentTimeMillis >= bundle.expiresAtMillis
    }

    fun isMalformed(): Boolean {
        return bundle.expiresAtMillis <= bundle.issuedAtMillis
    }

    fun classifyDomain(domain: DomainName): Set<DomainIntelligenceCategory> {
        return findRecord(domain)?.categories.orEmpty()
    }

    fun riskLevelFor(domain: DomainName): DomainRiskLevel {
        val record = findRecord(domain) ?: return DomainRiskLevel.UNKNOWN
        if (record.riskLevel != DomainRiskLevel.UNKNOWN) {
            return record.riskLevel
        }
        return if (record.categories.any { category -> category in highRiskCategories }) {
            DomainRiskLevel.HIGH
        } else {
            DomainRiskLevel.UNKNOWN
        }
    }

    private fun matches(candidate: DomainName, rule: DomainName): Boolean {
        return candidate == rule || candidate.value.endsWith(".${rule.value}")
    }

    private companion object {
        val highRiskCategories = setOf(
            DomainIntelligenceCategory.PROXY_ANONYMIZER,
            DomainIntelligenceCategory.PRIVATE_DNS,
            DomainIntelligenceCategory.VPN_INFRASTRUCTURE,
        )
    }
}
