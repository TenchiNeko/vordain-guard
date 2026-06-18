package com.vordain.guard.vpn.service

import com.vordain.guard.vpn.session.VpnSessionSnapshot
import com.vordain.guard.vpn.session.VpnSessionState
import com.vordain.guard.vpn.session.VpnSessionStateReason
import com.vordain.guard.vpn.session.VpnSessionStateReducer

object DefaultServiceVpnSessionSinkFactory {
    fun create(): VpnSessionSink {
        return ServiceVpnSessionSink(
            stateReducer = VpnSessionStateReducer(),
            initialSnapshot = VpnSessionSnapshot(
                state = VpnSessionState.READY,
                reason = VpnSessionStateReason.USER_PERMISSION_GRANTED,
                updatedAtMillis = 0L,
                message = null,
            ),
            clock = SystemVpnSessionClock,
        )
    }
}

private object SystemVpnSessionClock : VpnSessionClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
