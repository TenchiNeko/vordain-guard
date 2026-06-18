package com.vordain.guard.vpn.engine

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName

interface CompatibilityDecisionCache {
    fun get(
        appPackageName: AppPackageName,
        domainName: DomainName,
        policyVersion: String?,
        currentTimeMillis: Long,
    ): CompatibilityCacheEntry?

    fun put(entry: CompatibilityCacheEntry)

    fun removeExpired(currentTimeMillis: Long)
}
