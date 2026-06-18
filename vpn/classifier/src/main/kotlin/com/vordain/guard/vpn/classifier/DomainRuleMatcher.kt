package com.vordain.guard.vpn.classifier

import com.vordain.guard.core.model.DomainName

internal object DomainRuleMatcher {
    fun matches(candidate: DomainName, rule: DomainName): Boolean {
        return candidate == rule || candidate.value.endsWith(".${rule.value}")
    }
}
