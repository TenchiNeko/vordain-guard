package com.vordain.guard.vpn.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VpnTunnelSpecTest {
    @Test
    fun rejectsIpv4DefaultRoute() {
        assertFailsWith<IllegalArgumentException> {
            VpnTunnelRoute(address = "0.0.0.0", prefixLength = 0)
        }
    }

    @Test
    fun rejectsIpv6DefaultRoute() {
        assertFailsWith<IllegalArgumentException> {
            VpnTunnelRoute(address = "::", prefixLength = 0)
        }
    }

    @Test
    fun acceptsSafeSmokeTestRoute() {
        val route = VpnTunnelRoute(address = "192.0.2.0", prefixLength = 24)

        assertEquals("192.0.2.0", route.address)
        assertEquals(24, route.prefixLength)
    }

    @Test
    fun rejectsBlankSessionName() {
        assertFailsWith<IllegalArgumentException> {
            VpnTunnelSpec(
                sessionName = " ",
                addresses = listOf(VpnTunnelAddress(address = "10.111.0.2", prefixLength = 32)),
                routes = emptyList(),
            )
        }
    }

    @Test
    fun requiresLocalAddress() {
        assertFailsWith<IllegalArgumentException> {
            VpnTunnelSpec(
                sessionName = "Vordain Guard VPN shell",
                addresses = emptyList(),
                routes = emptyList(),
            )
        }
    }

    @Test
    fun doesNotAllowNormalTrafficRouting() {
        assertFailsWith<IllegalArgumentException> {
            VpnTunnelSpec(
                sessionName = "Vordain Guard VPN shell",
                addresses = listOf(VpnTunnelAddress(address = "10.111.0.2", prefixLength = 32)),
                routes = emptyList(),
                allowNormalTrafficRouting = true,
            )
        }
    }

    @Test
    fun smokeTestSpecUsesSafeRouteOnly() {
        val spec = VpnTunnelSpec.establishOnlySmokeTest()

        assertFalse(spec.allowNormalTrafficRouting)
        assertFalse(spec.labMode)
        assertTrue(spec.addresses.isNotEmpty())
        assertEquals(listOf(VpnTunnelRoute(address = "192.0.2.0", prefixLength = 24)), spec.routes)
    }

    @Test
    fun specDoesNotModelDnsSettings() {
        val spec = VpnTunnelSpec.establishOnlySmokeTest()

        assertFalse(spec.toString().contains("dns", ignoreCase = true))
    }

    @Test
    fun labFullTunnelSpecAllowsDefaultRoutesOnlyInLabMode() {
        val spec = VpnTunnelSpec.labFullTunnelCapture()

        assertTrue(spec.labMode)
        assertTrue(spec.allowNormalTrafficRouting)
        assertTrue(spec.routes.contains(VpnTunnelRoute("0.0.0.0", 0, allowDefaultRoute = true)))
        assertTrue(spec.routes.contains(VpnTunnelRoute("::", 0, allowDefaultRoute = true)))
    }

    @Test
    fun safeSmokeTestSpecDoesNotUseDefaultRoutes() {
        val spec = VpnTunnelSpec.establishOnlySmokeTest()

        assertFalse(spec.routes.any { it.address == "0.0.0.0" && it.prefixLength == 0 })
        assertFalse(spec.routes.any { it.address == "::" && it.prefixLength == 0 })
    }
}
