package com.vordain.guard.vpn.engine

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName

class InMemoryCompatibilityDecisionCache : CompatibilityDecisionCache {
    private val entriesByKey = linkedMapOf<CompatibilityCacheKey, CompatibilityCacheEntry>()

    override fun get(
        appPackageName: AppPackageName,
        domainName: DomainName,
        policyVersion: String?,
        currentTimeMillis: Long,
    ): CompatibilityCacheEntry? {
        val key = CompatibilityCacheKey(appPackageName, domainName, policyVersion)
        val entry = entriesByKey[key] ?: return null
        if (entry.expiresAtMillis <= currentTimeMillis) {
            entriesByKey.remove(key)
            return null
        }
        return entry
    }

    override fun put(entry: CompatibilityCacheEntry) {
        entriesByKey[CompatibilityCacheKey(entry.appPackageName, entry.domainName, entry.policyVersion)] = entry
    }

    override fun removeExpired(currentTimeMillis: Long) {
        entriesByKey.entries.removeIf { (_, entry) -> entry.expiresAtMillis <= currentTimeMillis }
    }

    private data class CompatibilityCacheKey(
        val appPackageName: AppPackageName,
        val domainName: DomainName,
        val policyVersion: String?,
    )
}
