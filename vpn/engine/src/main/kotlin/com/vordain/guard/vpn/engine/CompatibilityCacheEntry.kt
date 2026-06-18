package com.vordain.guard.vpn.engine

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName

data class CompatibilityCacheEntry(
    val appPackageName: AppPackageName,
    val domainName: DomainName,
    val policyVersion: String?,
    val action: TrafficAction,
    val reason: AppAwareTrafficGateReason,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
)
