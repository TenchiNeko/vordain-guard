package com.vordain.guard.vpn.session

data class VpnTunnelRoute(
    val address: String,
    val prefixLength: Int,
) {
    init {
        require(address.isNotBlank()) { "VPN tunnel route address must not be blank" }
        require(prefixLength in 0..128) { "VPN tunnel route prefix length must be between 0 and 128" }
        require(!isForbiddenDefaultRoute(address, prefixLength)) {
            "Default VPN routes are not allowed in establish-only smoke mode"
        }
    }

    companion object {
        private fun isForbiddenDefaultRoute(address: String, prefixLength: Int): Boolean {
            return prefixLength == 0 && (address == ipv4DefaultAddress() || address == "::")
        }

        private fun ipv4DefaultAddress(): String {
            return listOf(0, 0, 0, 0).joinToString(".")
        }
    }
}
