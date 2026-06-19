package com.vordain.guard.core.policypresets

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolicyPresetFactoryTest {
    private val factory = PolicyPresetFactory()

    @Test
    fun basicDnsGuardBlocksProxyAndEncryptedDnsSetting() {
        val definition = factory.describe(PolicyPreset.BASIC_DNS_GUARD)
        val policy = factory.createPolicy(PolicyPreset.BASIC_DNS_GUARD)

        assertTrue(definition.blockKnownProxyDomains)
        assertTrue(definition.blockEncryptedDnsResolvers)
        assertTrue(policy.blockKnownProxyDomains)
        assertFalse(policy.blockUnknownDomains)
    }

    @Test
    fun strictBrowserIsStricterThanBasic() {
        val basic = factory.describe(PolicyPreset.BASIC_DNS_GUARD)
        val strict = factory.describe(PolicyPreset.STRICT_BROWSER)

        assertFalse(basic.blockUnknownDomains)
        assertTrue(strict.blockUnknownDomains)
    }

    @Test
    fun highRiskLockdownIsStrictest() {
        val lockdown = factory.describe(PolicyPreset.HIGH_RISK_LOCKDOWN)

        assertTrue(lockdown.blockKnownProxyDomains)
        assertTrue(lockdown.blockEncryptedDnsResolvers)
        assertTrue(lockdown.blockUnknownDomains)
        assertTrue(lockdown.defaultAllowedDomains.size < factory.describe(PolicyPreset.SCHOOL_FRIENDLY).defaultAllowedDomains.size)
    }

    @Test
    fun customHasStableBaseline() {
        val first = factory.createPolicy(PolicyPreset.CUSTOM)
        val second = factory.createPolicy(PolicyPreset.CUSTOM)

        assertEquals(first, second)
    }

    @Test
    fun presetDefinitionsHaveDisplayText() {
        PolicyPreset.entries.forEach { preset ->
            val definition = factory.describe(preset)
            assertTrue(definition.displayName.isNotBlank())
            assertTrue(definition.description.isNotBlank())
            assertTrue(definition.warningText.contains("Not full protection"))
        }
    }

    @Test
    fun sourceHasNoForbiddenImportsOrGodClassNames() {
        val sourceRoot = repositoryRoot().resolve("core/policy-presets/src/main")
        val source = sourceRoot.walkTopDown().filter { it.isFile }.joinToString("\n") { it.readText() }

        assertFalse(source.contains("android" + "."))
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
