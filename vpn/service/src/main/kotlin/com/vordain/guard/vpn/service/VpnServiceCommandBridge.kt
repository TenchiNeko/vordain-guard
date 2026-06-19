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
            VordainVpnServiceActions.ACTION_START_DNS_ONLY_LAB -> {
                sessionSink.onVpnStartRequested()
                VpnServiceCommandResult.HandledDnsOnlyLabStart
            }
            VordainVpnServiceActions.ACTION_START_BASIC_DNS_GUARD -> {
                sessionSink.onVpnStartRequested()
                VpnServiceCommandResult.HandledBasicDnsGuardStart
            }
            VordainVpnServiceActions.ACTION_STOP_PROTECTION -> {
                sessionSink.onVpnStopRequested()
                VpnServiceCommandResult.HandledStop
            }
            VordainVpnServiceActions.ACTION_STOP_LAB_CAPTURE -> {
                sessionSink.onVpnStopRequested()
                VpnServiceCommandResult.HandledStop
            }
            VordainVpnServiceActions.ACTION_STOP_DNS_ONLY_LAB -> {
                sessionSink.onVpnStopRequested()
                VpnServiceCommandResult.HandledStop
            }
            VordainVpnServiceActions.ACTION_STOP_BASIC_DNS_GUARD -> {
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
    HandledDnsOnlyLabStart,
    HandledBasicDnsGuardStart,
    HandledStop,
    Ignored,
}
