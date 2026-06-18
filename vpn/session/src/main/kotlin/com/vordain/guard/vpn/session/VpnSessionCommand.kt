package com.vordain.guard.vpn.session

sealed interface VpnSessionCommand {
    data class Prepare(val permissionGranted: Boolean) : VpnSessionCommand
    data object Start : VpnSessionCommand
    data object MarkStarted : VpnSessionCommand
    data object Stop : VpnSessionCommand
    data object MarkStopped : VpnSessionCommand
    data object MarkRevoked : VpnSessionCommand
    data class MarkError(val message: String?) : VpnSessionCommand
}
