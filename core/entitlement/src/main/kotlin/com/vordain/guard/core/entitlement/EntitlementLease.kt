package com.vordain.guard.core.entitlement

data class EntitlementLease(
    val accountId: AccountId,
    val tier: EntitlementTier,
    val features: Set<EntitlementFeature>,
    val maxChildDevices: Int,
    val issuedAtMillis: Long,
    val expiresAtMillis: Long,
    val signature: EntitlementSignature,
)
