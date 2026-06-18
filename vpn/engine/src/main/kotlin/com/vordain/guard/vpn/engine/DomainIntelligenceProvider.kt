package com.vordain.guard.vpn.engine

import com.vordain.guard.core.intelligence.DomainIntelligenceCategory
import com.vordain.guard.core.intelligence.DomainIntelligenceRecord
import com.vordain.guard.core.model.DomainName

interface DomainIntelligenceProvider {
    fun recordFor(domain: DomainName): DomainIntelligenceRecord?
    fun categoriesFor(domain: DomainName): Set<DomainIntelligenceCategory>
}
