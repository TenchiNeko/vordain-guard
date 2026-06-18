package com.vordain.guard.core.entitlement

object EntitlementDefaults {
    fun featuresFor(tier: EntitlementTier): Set<EntitlementFeature> {
        return when (tier) {
            EntitlementTier.FREE -> setOf(
                EntitlementFeature.LOCAL_VPN_FILTERING,
            )
            EntitlementTier.BASIC -> setOf(
                EntitlementFeature.LOCAL_VPN_FILTERING,
                EntitlementFeature.HEARTBEAT_ALERTS,
            )
            EntitlementTier.GUARD_PLUS -> guardPlusFeatures()
            EntitlementTier.MANAGED -> guardPlusFeatures() + EntitlementFeature.MANAGED_DEVICE_LOCKDOWN
        }
    }

    private fun guardPlusFeatures(): Set<EntitlementFeature> {
        return setOf(
            EntitlementFeature.LOCAL_VPN_FILTERING,
            EntitlementFeature.HEARTBEAT_ALERTS,
            EntitlementFeature.ENCRYPTED_PARENT_RELAY,
            EntitlementFeature.MULTI_CHILD_DEVICES,
            EntitlementFeature.PARENT_REVIEW_REQUESTS,
            EntitlementFeature.COMPATIBILITY_LEARNING,
        )
    }
}
