package com.vordain.guard.core.intelligence

data class IntelligenceBundle(
    val bundleVersion: String,
    val issuedAtMillis: Long,
    val expiresAtMillis: Long,
    val domainRecords: List<DomainIntelligenceRecord>,
    val appProfiles: List<AppCompatibilityProfile>,
    val signature: IntelligenceBundleSignature,
)
