package com.vordain.guard.vpn.engine

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName

data class TrafficObservation(
    val appPackageName: AppPackageName?,
    val domainName: DomainName?,
    val observedAtMillis: Long,
    val policyVersion: String?,
)
