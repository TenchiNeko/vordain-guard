package com.vordain.guard.core.intelligence

import com.vordain.guard.core.model.DomainName

enum class DnsBypassDomainClassification {
    DNS_OVER_HTTPS,
    PRIVATE_DNS,
}

data class EncryptedDnsResolverMatch(
    val matchedDomain: DomainName,
    val classification: DnsBypassDomainClassification,
    val reason: String,
)

class EncryptedDnsResolverSeedList(
    private val resolverDomains: Set<DomainName> = NON_EXHAUSTIVE_LAB_SEED_DOMAINS,
) {
    fun classify(domain: DomainName): EncryptedDnsResolverMatch? {
        val matched = resolverDomains.firstOrNull { seed ->
            domain.value == seed.value || domain.value.endsWith(".${seed.value}")
        } ?: return null
        return EncryptedDnsResolverMatch(
            matchedDomain = matched,
            classification = DnsBypassDomainClassification.DNS_OVER_HTTPS,
            reason = "Encrypted DNS resolver blocked in DNS-only lab",
        )
    }

    companion object {
        val NON_EXHAUSTIVE_LAB_SEED_DOMAINS = setOf(
            DomainName.from("dns.google"),
            DomainName.from("cloudflare-dns.com"),
            DomainName.from("one.one.one.one"),
            DomainName.from("dns.quad9.net"),
            DomainName.from("dns.nextdns.io"),
            DomainName.from("doh.opendns.com"),
        )

        const val SOURCE_LABEL = "non-exhaustive lab seed"
    }
}
