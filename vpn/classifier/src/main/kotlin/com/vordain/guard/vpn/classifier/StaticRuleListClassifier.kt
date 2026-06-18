package com.vordain.guard.vpn.classifier

import com.vordain.guard.core.model.DomainCategory
import com.vordain.guard.core.model.DomainClassification
import com.vordain.guard.core.model.DomainName

class StaticRuleListClassifier(
    private val proxyAnonymizerRules: Set<DomainName>,
) : DomainClassifier {
    override fun classify(domain: DomainName): DomainClassification {
        return if (proxyAnonymizerRules.any { rule -> DomainRuleMatcher.matches(candidate = domain, rule = rule) }) {
            DomainClassification.of(DomainCategory.PROXY_ANONYMIZER)
        } else {
            DomainClassification.Unknown
        }
    }
}
