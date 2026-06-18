package com.vordain.guard.vpn.dns

import com.vordain.guard.core.model.DomainName

data class DnsObservation(
    val domain: DomainName,
    val observedAtMillis: Long,
)
