package com.vordain.guard.core.intelligence

import com.vordain.guard.core.model.DomainName
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EncryptedDnsResolverSeedListTest {
    private val seedList = EncryptedDnsResolverSeedList()

    @Test
    fun dnsGoogleMatchesEncryptedDnsResolver() {
        assertNotNull(seedList.classify(DomainName.from("dns.google")))
    }

    @Test
    fun subdomainOfCloudflareDnsMatches() {
        assertNotNull(seedList.classify(DomainName.from("security.cloudflare-dns.com")))
    }

    @Test
    fun suffixTrapDoesNotMatch() {
        assertNull(seedList.classify(DomainName.from("evilcloudflare-dns.com")))
    }

    @Test
    fun appendedDomainDoesNotMatch() {
        assertNull(seedList.classify(DomainName.from("dns.google.evil.example")))
    }

    @Test
    fun seedListIsNonEmptyAndLabeledNonExhaustive() {
        assertTrue(EncryptedDnsResolverSeedList.NON_EXHAUSTIVE_LAB_SEED_DOMAINS.isNotEmpty())
        assertTrue(EncryptedDnsResolverSeedList.SOURCE_LABEL.contains("non-exhaustive lab seed"))
    }

    @Test
    fun sourceHasNoForbiddenImportsOrGodClassNames() {
        val sourceRoot = repositoryRoot().resolve("core/intelligence/src/main")
        val source = sourceRoot.walkTopDown().filter { it.isFile }.joinToString("\n") { it.readText() }

        assertFalse(source.contains("android" + "."))
        assertFalse(source.contains("backend"))
        assertFalse(source.contains("data.relay"))
        assertFalse(source.contains("data.outbox"))
        assertFalse(Regex("class .*" + "Man" + "ager|object .*" + "Man" + "ager").containsMatchIn(source))
        assertFalse(source.contains("TO" + "DO"))
        assertFalse(source.contains("FIX" + "ME"))
    }

    private fun repositoryRoot(): File {
        var current = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) return current
            current = current.parentFile ?: error("Could not find repository root")
        }
    }
}
