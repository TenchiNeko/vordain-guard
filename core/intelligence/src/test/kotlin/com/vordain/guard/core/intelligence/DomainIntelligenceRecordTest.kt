package com.vordain.guard.core.intelligence

import com.vordain.guard.core.model.DomainName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DomainIntelligenceRecordTest {
    @Test
    fun validDomainIntelligenceRecordCanBeCreated() {
        val record = record("proxy.example")

        assertEquals(DomainName.from("proxy.example"), record.domain)
        assertEquals(setOf(DomainIntelligenceCategory.PROXY_ANONYMIZER), record.categories)
        assertEquals(DomainRiskLevel.UNKNOWN, record.riskLevel)
        assertEquals(95, record.confidence)
        assertEquals(IntelligenceSource.HUMAN_REVIEW, record.source)
        assertEquals(1_000L, record.reviewedAtMillis)
    }

    @Test
    fun invalidConfidenceIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            record("proxy.example", confidence = 101)
        }
    }

    private fun record(
        domain: String,
        confidence: Int = 95,
    ): DomainIntelligenceRecord {
        return DomainIntelligenceRecord(
            domain = DomainName.from(domain),
            categories = setOf(DomainIntelligenceCategory.PROXY_ANONYMIZER),
            riskLevel = DomainRiskLevel.UNKNOWN,
            confidence = confidence,
            source = IntelligenceSource.HUMAN_REVIEW,
            reviewedAtMillis = 1_000L,
        )
    }
}
