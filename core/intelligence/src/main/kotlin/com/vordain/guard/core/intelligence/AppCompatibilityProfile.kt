package com.vordain.guard.core.intelligence

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName

data class AppCompatibilityProfile(
    val appPackageName: AppPackageName,
    val displayName: String?,
    val requiredDomains: Set<DomainName>,
    val optionalDomains: Set<DomainName>,
    val blockedRegardlessDomains: Set<DomainName>,
    val profileVersion: String,
    val confidence: Int,
) {
    init {
        require(confidence in 0..100) { "confidence must be in 0..100" }
    }
}
