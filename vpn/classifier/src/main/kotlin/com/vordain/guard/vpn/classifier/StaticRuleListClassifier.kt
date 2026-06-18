package com.vordain.guard.vpn.classifier

import com.vordain.guard.core.model.DomainName

class StaticRuleListClassifier(
    private val categoriesByDomain: Map<DomainName, Set<Category>>,
) : DomainClassifier {
    override fun classify(domain: DomainName): Set<Category> {
        return categoriesByDomain[domain] ?: setOf(Category.UNKNOWN)
    }
}
