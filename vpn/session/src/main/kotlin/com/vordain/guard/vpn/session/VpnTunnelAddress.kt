package com.vordain.guard.vpn.session

data class VpnTunnelAddress(
    val address: String,
    val prefixLength: Int,
) {
    init {
        require(address.isNotBlank()) { "VPN tunnel address must not be blank" }
        require(prefixLength in 0..128) { "VPN tunnel address prefix length must be between 0 and 128" }
    }
}
