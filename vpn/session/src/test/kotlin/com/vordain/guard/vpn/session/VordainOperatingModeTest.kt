package com.vordain.guard.vpn.session

import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VordainOperatingModeTest {
    @Test
    fun basicDnsGuardDoesNotUseDefaultRoutes() {
        val label = VordainOperatingModeLabels.describe(VordainOperatingMode.BASIC_DNS_GUARD)

        assertFalse(label.usesDefaultRoute)
    }

    @Test
    fun fullTunnelLabIsOnlyModeWithDefaultRoute() {
        val defaultRouteModes = VordainOperatingMode.entries
            .filter { mode -> VordainOperatingModeLabels.describe(mode).usesDefaultRoute }

        assertTrue(defaultRouteModes == listOf(VordainOperatingMode.FULL_TUNNEL_LAB))
    }

    @Test
    fun basicDnsGuardIsNotProductionReady() {
        val label = VordainOperatingModeLabels.describe(VordainOperatingMode.BASIC_DNS_GUARD)

        assertFalse(label.productionReady)
    }

    @Test
    fun basicDnsGuardSaysNonDnsTrafficIsNotInspected() {
        val label = VordainOperatingModeLabels.describe(VordainOperatingMode.BASIC_DNS_GUARD)

        assertTrue(label.warningText.contains("Non-DNS traffic is not inspected"))
        assertFalse(label.inspectsNonDnsTraffic)
        assertFalse(label.forwardsNonDnsTraffic)
    }

    @Test
    fun operatingModeLabelsDoNotUseRestrictedProductClaims() {
        val labels = VordainOperatingMode.entries
            .map(VordainOperatingModeLabels::describe)
            .joinToString(separator = "\n") { label -> "${label.displayName}\n${label.warningText}" }

        assertFalse(labels.contains("Protected"))
        assertFalse(labels.contains("Filtering active"))
        assertFalse(labels.contains("unbypassable", ignoreCase = true))
        assertFalse(labels.contains("guaranteed", ignoreCase = true))
    }

    @Test
    fun sourceHasNoAndroidImportsOrManagerNamesOrTodos() {
        val source = Files.walk(Path("src/main"))
            .filter(Files::isRegularFile)
            .map { Files.readString(it) }
            .toList()
            .joinToString(separator = "\n")

        assertFalse(source.contains("android" + "."))
        assertFalse(Regex("""\b(class|object)\s+\w*Manager\b""").containsMatchIn(source))
        assertFalse(source.contains("TO" + "DO"))
        assertFalse(source.contains("FIX" + "ME"))
    }
}
