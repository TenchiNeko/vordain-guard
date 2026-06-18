package com.vordain.guard.core.entitlement

class EntitlementEvaluator {
    fun evaluate(
        lease: EntitlementLease,
        currentTimeMillis: Long,
    ): EntitlementEvaluation {
        val inactiveReason = inactiveReasonFor(lease, currentTimeMillis)
        if (inactiveReason != null) {
            return EntitlementEvaluation(
                active = false,
                tier = lease.tier,
                enabledFeatures = emptySet(),
                reason = inactiveReason,
            )
        }

        return EntitlementEvaluation(
            active = true,
            tier = lease.tier,
            enabledFeatures = lease.features,
            reason = EntitlementEvaluationReason.ACTIVE,
        )
    }

    fun isFeatureEnabled(
        lease: EntitlementLease,
        feature: EntitlementFeature,
        currentTimeMillis: Long,
    ): Boolean {
        val evaluation = evaluate(lease, currentTimeMillis)
        return evaluation.active && feature in evaluation.enabledFeatures
    }

    private fun inactiveReasonFor(
        lease: EntitlementLease,
        currentTimeMillis: Long,
    ): EntitlementEvaluationReason? {
        return when {
            lease.signature.value.isBlank() -> EntitlementEvaluationReason.INVALID_SIGNATURE_FORMAT
            lease.maxChildDevices < 0 -> EntitlementEvaluationReason.INVALID_DEVICE_LIMIT
            lease.expiresAtMillis <= lease.issuedAtMillis -> EntitlementEvaluationReason.MALFORMED_LEASE
            currentTimeMillis < lease.issuedAtMillis -> EntitlementEvaluationReason.NOT_YET_VALID
            currentTimeMillis >= lease.expiresAtMillis -> EntitlementEvaluationReason.EXPIRED
            else -> null
        }
    }
}
