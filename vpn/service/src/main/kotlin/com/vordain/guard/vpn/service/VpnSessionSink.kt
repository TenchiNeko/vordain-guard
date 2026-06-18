package com.vordain.guard.vpn.service

interface VpnSessionSink {
    fun onVpnStartRequested()
    fun onVpnStarted()
    fun onVpnStopRequested()
    fun onVpnStopped()
    fun onVpnRevoked()

    companion object {
        val NoOp = object : VpnSessionSink {
            override fun onVpnStartRequested() = Unit
            override fun onVpnStarted() = Unit
            override fun onVpnStopRequested() = Unit
            override fun onVpnStopped() = Unit
            override fun onVpnRevoked() = Unit
        }
    }
}
