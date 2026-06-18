package com.vordain.guard.vpn.lifecycle

interface VpnLifecycleIncidentHandler {
    fun handle(incident: VpnLifecycleIncident): VpnLifecycleIncidentResult
}
