package com.vordain.guard.vpn.service

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.vpn.lifecycle.VpnLifecycleIncident
import com.vordain.guard.vpn.lifecycle.VpnLifecycleIncidentHandler
import com.vordain.guard.vpn.lifecycle.VpnLifecycleIncidentResult
import com.vordain.guard.vpn.lifecycle.VpnLifecycleIncidentType

class ServiceVpnLifecycleSink(
    private val deviceIdProvider: VpnLifecycleDeviceIdProvider,
    private val clock: VpnLifecycleClock,
    private val incidentHandler: VpnLifecycleIncidentHandler,
) : VpnLifecycleSink {
    var lastResult: VpnLifecycleIncidentResult? = null
        private set

    override fun onVpnStarted() {
        report(VpnLifecycleIncidentType.VPN_STARTED)
    }

    override fun onVpnStopped() {
        report(VpnLifecycleIncidentType.VPN_STOPPED)
    }

    override fun onVpnRevoked() {
        report(VpnLifecycleIncidentType.VPN_REVOKED)
    }

    private fun report(type: VpnLifecycleIncidentType) {
        lastResult = incidentHandler.handle(
            VpnLifecycleIncident(
                deviceId = deviceIdProvider.currentDeviceId(),
                type = type,
                occurredAtMillis = clock.nowMillis(),
                summary = null,
            ),
        )
    }
}

interface VpnLifecycleDeviceIdProvider {
    fun currentDeviceId(): DeviceId
}

interface VpnLifecycleClock {
    fun nowMillis(): Long
}
