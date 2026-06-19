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
            VordainVpnServiceActions.ACTION_START_LAB_CAPTURE -> {
                sessionSink.onVpnStartRequested()
                VpnServiceCommandResult.HandledLabStart
            }
            VordainVpnServiceActions.ACTION_STOP_PROTECTION -> {
                sessionSink.onVpnStopRequested()
                VpnServiceCommandResult.HandledStop
            }
            VordainVpnServiceActions.ACTION_STOP_LAB_CAPTURE -> {
                sessionSink.onVpnStopRequested()
                VpnServiceCommandResult.HandledStop
            }
            else -> VpnServiceCommandResult.Ignored
        }
    }
}

enum class VpnServiceCommandResult {
    HandledStart,
    HandledLabStart,
    HandledStop,
    Ignored,
}
