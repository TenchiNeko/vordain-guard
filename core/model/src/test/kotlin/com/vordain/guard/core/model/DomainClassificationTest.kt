package com.vordain.guard.core.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DomainClassificationTest {
    @Test
    fun unknownClassificationContainsUnknownCategoryOnly() {
        assertTrue(DomainClassification.Unknown.contains(DomainCategory.UNKNOWN))
        assertFalse(DomainClassification.Unknown.contains(DomainCategory.PROXY_ANONYMIZER))
    }

    @Test
    fun classificationContainsProvidedCategory() {
        val classification = DomainClassification.of(DomainCategory.PROXY_ANONYMIZER)

        assertTrue(classification.contains(DomainCategory.PROXY_ANONYMIZER))
        assertFalse(classification.contains(DomainCategory.UNKNOWN))
    }
}
