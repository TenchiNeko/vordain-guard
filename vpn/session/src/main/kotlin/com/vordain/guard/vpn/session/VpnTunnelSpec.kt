package com.vordain.guard.vpn.session

data class VpnTunnelSpec(
    val sessionName: String,
    val addresses: List<VpnTunnelAddress>,
    val routes: List<VpnTunnelRoute>,
    val dnsServers: List<String> = emptyList(),
    val allowNormalTrafficRouting: Boolean = false,
    val labMode: Boolean = false,
    val modeLabel: String = "ESTABLISH_ONLY",
) {
    init {
        require(sessionName.isNotBlank()) { "VPN tunnel session name must not be blank" }
        require(addresses.isNotEmpty()) { "VPN tunnel requires at least one local address" }
        require(!allowNormalTrafficRouting || labMode) {
            "Normal traffic routing is disabled for establish-only smoke mode"
        }
        require(routes.none { it.allowDefaultRoute } || labMode) {
            "Default VPN routes are allowed only in explicit lab capture mode"
        }
    }

    companion object {
        fun establishOnlySmokeTest(): VpnTunnelSpec {
            return VpnTunnelSpec(
                sessionName = "Vordain Guard VPN shell",
                addresses = listOf(VpnTunnelAddress(address = "10.111.0.2", prefixLength = 32)),
                routes = listOf(VpnTunnelRoute(address = "192.0.2.0", prefixLength = 24)),
            )
        }

        fun labFullTunnelCapture(): VpnTunnelSpec {
            return VpnTunnelSpec(
                sessionName = "Vordain Guard Lab Capture",
                addresses = listOf(VpnTunnelAddress(address = "10.111.0.2", prefixLength = 32)),
                routes = listOf(
                    VpnTunnelRoute(address = "0.0.0.0", prefixLength = 0, allowDefaultRoute = true),
                    VpnTunnelRoute(address = "::", prefixLength = 0, allowDefaultRoute = true),
                ),
                allowNormalTrafficRouting = true,
                labMode = true,
                modeLabel = "FULL_TUNNEL_LAB",
            )
        }

        fun dnsOnlyLabFiltering(): VpnTunnelSpec {
            return VpnTunnelSpec(
                sessionName = "Vordain Guard DNS Lab",
                addresses = listOf(VpnTunnelAddress(address = "10.111.0.2", prefixLength = 32)),
                routes = listOf(VpnTunnelRoute(address = DNS_ONLY_LAB_DNS_SERVER, prefixLength = 32)),
                dnsServers = listOf(DNS_ONLY_LAB_DNS_SERVER),
                labMode = true,
                modeLabel = "DNS_ONLY_LAB",
            )
        }

        const val DNS_ONLY_LAB_DNS_SERVER = "10.111.0.1"
    }
}
