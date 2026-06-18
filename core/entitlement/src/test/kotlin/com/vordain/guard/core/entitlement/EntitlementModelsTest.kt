package com.vordain.guard.core.entitlement

import kotlin.test.Test
import kotlin.test.assertEquals

class EntitlementModelsTest {
    @Test
    fun validLeaseCanBeCreated() {
        val lease = lease()

        assertEquals(AccountId("account-1"), lease.accountId)
        assertEquals(EntitlementTier.BASIC, lease.tier)
        assertEquals(setOf(EntitlementFeature.LOCAL_VPN_FILTERING), lease.features)
        assertEquals(1, lease.maxChildDevices)
        assertEquals(1_000L, lease.issuedAtMillis)
        assertEquals(2_000L, lease.expiresAtMillis)
        assertEquals(EntitlementSignature("signature-1"), lease.signature)
    }

    @Test
    fun blankSignatureEvaluatesInvalid() {
        val evaluation = evaluator.evaluate(lease(signature = ""), currentTimeMillis = 1_500L)

        assertEquals(false, evaluation.active)
        assertEquals(EntitlementEvaluationReason.INVALID_SIGNATURE_FORMAT, evaluation.reason)
    }

    @Test
    fun negativeMaxChildDevicesEvaluatesInvalid() {
        val evaluation = evaluator.evaluate(lease(maxChildDevices = -1), currentTimeMillis = 1_500L)

        assertEquals(false, evaluation.active)
        assertEquals(EntitlementEvaluationReason.INVALID_DEVICE_LIMIT, evaluation.reason)
    }

    @Test
    fun expiryEqualToIssuedTimeEvaluatesMalformed() {
        val evaluation = evaluator.evaluate(lease(issuedAtMillis = 1_000L, expiresAtMillis = 1_000L), currentTimeMillis = 1_000L)

        assertEquals(false, evaluation.active)
        assertEquals(EntitlementEvaluationReason.MALFORMED_LEASE, evaluation.reason)
    }

    @Test
    fun expiryBeforeIssuedTimeEvaluatesMalformed() {
        val evaluation = evaluator.evaluate(lease(issuedAtMillis = 2_000L, expiresAtMillis = 1_000L), currentTimeMillis = 1_500L)

        assertEquals(false, evaluation.active)
        assertEquals(EntitlementEvaluationReason.MALFORMED_LEASE, evaluation.reason)
    }

    private fun lease(
        maxChildDevices: Int = 1,
        issuedAtMillis: Long = 1_000L,
        expiresAtMillis: Long = 2_000L,
        signature: String = "signature-1",
    ): EntitlementLease {
        return EntitlementLease(
            accountId = AccountId("account-1"),
            tier = EntitlementTier.BASIC,
            features = setOf(EntitlementFeature.LOCAL_VPN_FILTERING),
            maxChildDevices = maxChildDevices,
            issuedAtMillis = issuedAtMillis,
            expiresAtMillis = expiresAtMillis,
            signature = EntitlementSignature(signature),
        )
    }

    private companion object {
        val evaluator = EntitlementEvaluator()
    }
}
