package com.vordain.guard.core.entitlement

data class EntitlementEvaluation(
    val active: Boolean,
    val tier: EntitlementTier,
    val enabledFeatures: Set<EntitlementFeature>,
    val reason: EntitlementEvaluationReason,
)
