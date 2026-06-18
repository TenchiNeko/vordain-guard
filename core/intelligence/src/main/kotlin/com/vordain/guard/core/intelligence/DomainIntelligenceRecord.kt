package com.vordain.guard.core.intelligence

import com.vordain.guard.core.model.DomainName

data class DomainIntelligenceRecord(
    val domain: DomainName,
    val categories: Set<DomainIntelligenceCategory>,
    val riskLevel: DomainRiskLevel,
    val confidence: Int,
    val source: IntelligenceSource,
    val reviewedAtMillis: Long?,
) {
    init {
        require(confidence in 0..100) { "confidence must be in 0..100" }
    }
}
