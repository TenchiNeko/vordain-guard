package com.vordain.guard.vpn.service

import android.net.VpnService
import com.vordain.guard.core.policy.Policy
import com.vordain.guard.vpn.lab.InMemoryLabTrafficObserver
import com.vordain.guard.vpn.lab.LabDnsForwardingMode
import com.vordain.guard.vpn.lab.LabTrafficObservationStats
import com.vordain.guard.vpn.lab.LabTrafficObserver
import com.vordain.guard.vpn.session.LabCaptureWatchdogState

object LabCaptureDebugStatus {
    private val observer = InMemoryLabTrafficObserver()
    @Volatile
    private var watchdogState: LabCaptureWatchdogState = LabCaptureWatchdogState.inactive()

    fun observer(): LabTrafficObserver = observer

    fun snapshot(): LabTrafficObservationStats = observer.snapshot()

    fun watchdogState(): LabCaptureWatchdogState = watchdogState

    fun watchdogLabel(): String {
        val state = watchdogState
        return if (state.active) {
            "active until ${state.expiresAtMillis}"
        } else {
            state.reason
        }
    }

    fun updateWatchdogState(state: LabCaptureWatchdogState) {
        watchdogState = state
    }

    fun configureProtectedDnsUpstream(vpnService: VpnService) {
        observer.configureUpstream(
            forwardingMode = LabDnsForwardingMode.LAB_UPSTREAM,
            upstreamTransport = AndroidProtectedUdpDnsTransport(vpnService),
        )
    }

    fun useVerifiedPolicy(policy: Policy) {
        observer.updatePolicy(policy)
    }

    fun useDefaultPolicy() {
        observer.useDefaultPolicy()
    }
}
