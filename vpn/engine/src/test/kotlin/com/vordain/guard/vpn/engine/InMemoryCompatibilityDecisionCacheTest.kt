package com.vordain.guard.vpn.engine

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryCompatibilityDecisionCacheTest {
    @Test
    fun storesAndReturnsEntriesByAppDomainAndPolicyVersion() {
        val cache = InMemoryCompatibilityDecisionCache()
        val entry = entry(app = "com.streaming.app", domain = "cdn.example", policyVersion = "policy-1")

        cache.put(entry)

        assertEquals(
            entry,
            cache.get(
                appPackageName = AppPackageName("com.streaming.app"),
                domainName = DomainName.from("cdn.example"),
                policyVersion = "policy-1",
                currentTimeMillis = 1_500L,
            ),
        )
    }

    @Test
    fun missesWhenPolicyVersionDiffers() {
        val cache = InMemoryCompatibilityDecisionCache()
        cache.put(entry(app = "com.streaming.app", domain = "cdn.example", policyVersion = "policy-1"))

        assertNull(
            cache.get(
                appPackageName = AppPackageName("com.streaming.app"),
                domainName = DomainName.from("cdn.example"),
                policyVersion = "policy-2",
                currentTimeMillis = 1_500L,
            ),
        )
    }

    @Test
    fun ignoresExpiredEntries() {
        val cache = InMemoryCompatibilityDecisionCache()
        cache.put(entry(app = "com.streaming.app", domain = "cdn.example", expiresAtMillis = 2_000L))

        assertNull(
            cache.get(
                appPackageName = AppPackageName("com.streaming.app"),
                domainName = DomainName.from("cdn.example"),
                policyVersion = "policy-1",
                currentTimeMillis = 2_000L,
            ),
        )
    }

    @Test
    fun distinguishesDifferentAppsForSameDomain() {
        val cache = InMemoryCompatibilityDecisionCache()
        cache.put(entry(app = "com.streaming.app", domain = "cdn.example"))

        assertNull(
            cache.get(
                appPackageName = AppPackageName("com.browser.app"),
                domainName = DomainName.from("cdn.example"),
                policyVersion = "policy-1",
                currentTimeMillis = 1_500L,
            ),
        )
    }

    private fun entry(
        app: String,
        domain: String,
        policyVersion: String? = "policy-1",
        expiresAtMillis: Long = 2_000L,
    ): CompatibilityCacheEntry {
        return CompatibilityCacheEntry(
            appPackageName = AppPackageName(app),
            domainName = DomainName.from(domain),
            policyVersion = policyVersion,
            action = TrafficAction.ALLOW,
            reason = AppAwareTrafficGateReason.COMPATIBILITY_LEARNED_ALLOW,
            createdAtMillis = 1_000L,
            expiresAtMillis = expiresAtMillis,
        )
    }
}
