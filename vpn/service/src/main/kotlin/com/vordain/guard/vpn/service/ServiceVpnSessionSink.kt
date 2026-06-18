package com.vordain.guard.vpn.service

import com.vordain.guard.vpn.session.VpnSessionCommand
import com.vordain.guard.vpn.session.VpnSessionSnapshot
import com.vordain.guard.vpn.session.VpnSessionStateReducer
import com.vordain.guard.vpn.session.VpnSessionTransition

class ServiceVpnSessionSink(
    private val stateReducer: VpnSessionStateReducer,
    initialSnapshot: VpnSessionSnapshot,
    private val clock: VpnSessionClock,
) : VpnSessionSink {
    var currentSnapshot: VpnSessionSnapshot = initialSnapshot
        private set

    var lastTransition: VpnSessionTransition? = null
        private set

    override fun onVpnStartRequested() {
        transition(VpnSessionCommand.Start)
    }

    override fun onVpnStarted() {
        transition(VpnSessionCommand.MarkStarted)
    }

    override fun onVpnStopRequested() {
        transition(VpnSessionCommand.Stop)
    }

    override fun onVpnStopped() {
        transition(VpnSessionCommand.MarkStopped)
    }

    override fun onVpnRevoked() {
        transition(VpnSessionCommand.MarkRevoked)
    }

    private fun transition(command: VpnSessionCommand) {
        val transition = stateReducer.reduce(
            previous = currentSnapshot,
            command = command,
            currentTimeMillis = clock.nowMillis(),
        )
        lastTransition = transition
        if (transition.accepted) {
            currentSnapshot = transition.next
        }
    }
}

interface VpnSessionClock {
    fun nowMillis(): Long
}
