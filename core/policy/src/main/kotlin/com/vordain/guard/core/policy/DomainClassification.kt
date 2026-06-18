package com.vordain.guard.core.policy

data class DomainClassification(
    val categories: Set<DomainCategory>,
) {
    companion object {
        val Unknown = DomainClassification(setOf(DomainCategory.UNKNOWN))

        fun of(vararg categories: DomainCategory): DomainClassification {
            return DomainClassification(categories.toSet())
        }
    }

    fun contains(category: DomainCategory): Boolean {
        return category in categories
    }
}
