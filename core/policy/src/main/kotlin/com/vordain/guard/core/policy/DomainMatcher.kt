package com.vordain.guard.core.policy

import com.vordain.guard.core.model.DomainName

object DomainMatcher {
    fun matches(candidate: DomainName, rule: DomainName): Boolean {
        return candidate == rule || candidate.value.endsWith(".${rule.value}")
    }
}
