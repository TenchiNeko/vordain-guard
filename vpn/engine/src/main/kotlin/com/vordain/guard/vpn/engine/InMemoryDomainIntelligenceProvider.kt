package com.vordain.guard.vpn.engine

import com.vordain.guard.core.intelligence.DomainIntelligenceCategory
import com.vordain.guard.core.intelligence.DomainIntelligenceRecord
import com.vordain.guard.core.model.DomainName

class InMemoryDomainIntelligenceProvider(
    private val records: List<DomainIntelligenceRecord>,
) : DomainIntelligenceProvider {
    override fun recordFor(domain: DomainName): DomainIntelligenceRecord? {
        return records.firstOrNull { record -> matches(candidate = domain, rule = record.domain) }
    }

    override fun categoriesFor(domain: DomainName): Set<DomainIntelligenceCategory> {
        return recordFor(domain)?.categories.orEmpty()
    }

    private fun matches(candidate: DomainName, rule: DomainName): Boolean {
        return candidate == rule || candidate.value.endsWith(".${rule.value}")
    }
}
