package com.vordain.guard.vpn.dns

import com.vordain.guard.core.model.DomainName

data class DnsQuestion(
    val domain: DomainName,
    val qtype: Int,
    val qclass: Int,
)
