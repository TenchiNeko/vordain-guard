package com.vordain.guard.core.entitlement

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntitlementDefaultsTest {
    @Test
    fun freeDefaultsAreExplicit() {
        assertEquals(
            setOf(EntitlementFeature.LOCAL_VPN_FILTERING),
            EntitlementDefaults.featuresFor(EntitlementTier.FREE),
        )
    }

    @Test
    fun basicIncludesHeartbeatAlerts() {
        assertTrue(EntitlementFeature.HEARTBEAT_ALERTS in EntitlementDefaults.featuresFor(EntitlementTier.BASIC))
    }

    @Test
    fun guardPlusIncludesRelayReviewAndCompatibilityLearning() {
        val features = EntitlementDefaults.featuresFor(EntitlementTier.GUARD_PLUS)

        assertTrue(EntitlementFeature.ENCRYPTED_PARENT_RELAY in features)
        assertTrue(EntitlementFeature.PARENT_REVIEW_REQUESTS in features)
        assertTrue(EntitlementFeature.COMPATIBILITY_LEARNING in features)
    }

    @Test
    fun managedIncludesManagedDeviceLockdown() {
        assertTrue(EntitlementFeature.MANAGED_DEVICE_LOCKDOWN in EntitlementDefaults.featuresFor(EntitlementTier.MANAGED))
    }

    @Test
    fun managedIncludesAllGuardPlusFeatures() {
        val guardPlus = EntitlementDefaults.featuresFor(EntitlementTier.GUARD_PLUS)
        val managed = EntitlementDefaults.featuresFor(EntitlementTier.MANAGED)

        assertTrue(managed.containsAll(guardPlus))
    }
}
