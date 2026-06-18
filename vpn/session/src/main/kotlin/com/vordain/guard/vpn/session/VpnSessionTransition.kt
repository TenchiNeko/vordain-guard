package com.vordain.guard.vpn.session

data class VpnSessionTransition(
    val previous: VpnSessionSnapshot,
    val command: VpnSessionCommand,
    val next: VpnSessionSnapshot,
    val accepted: Boolean,
)
