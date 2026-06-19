package com.vordain.guard.vpn.session

data class VpnTunnelSpec(
    val sessionName: String,
    val addresses: List<VpnTunnelAddress>,
    val routes: List<VpnTunnelRoute>,
    val allowNormalTrafficRouting: Boolean = false,
) {
    init {
        require(sessionName.isNotBlank()) { "VPN tunnel session name must not be blank" }
        require(addresses.isNotEmpty()) { "VPN tunnel requires at least one local address" }
        require(!allowNormalTrafficRouting) {
            "Normal traffic routing is disabled for establish-only smoke mode"
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
    }
}
