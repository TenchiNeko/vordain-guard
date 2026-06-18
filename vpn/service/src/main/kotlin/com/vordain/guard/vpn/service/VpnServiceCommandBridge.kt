package com.vordain.guard.vpn.service

class VpnServiceCommandBridge(
    private val sessionSink: VpnSessionSink,
) {
    fun handleAction(action: String?): VpnServiceCommandResult {
        return when (action) {
            VordainVpnServiceActions.ACTION_START_PROTECTION -> {
                sessionSink.onVpnStartRequested()
                VpnServiceCommandResult.HandledStart
            }
            VordainVpnServiceActions.ACTION_STOP_PROTECTION -> {
                sessionSink.onVpnStopRequested()
                VpnServiceCommandResult.HandledStop
            }
            else -> VpnServiceCommandResult.Ignored
        }
    }
}

enum class VpnServiceCommandResult {
    HandledStart,
    HandledStop,
    Ignored,
}
