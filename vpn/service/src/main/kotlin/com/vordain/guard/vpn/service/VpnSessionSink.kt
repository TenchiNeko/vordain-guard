package com.vordain.guard.vpn.service

interface VpnSessionSink {
    fun onVpnStarted()
    fun onVpnStopped()
    fun onVpnRevoked()

    companion object {
        val NoOp = object : VpnSessionSink {
            override fun onVpnStarted() = Unit
            override fun onVpnStopped() = Unit
            override fun onVpnRevoked() = Unit
        }
    }
}
