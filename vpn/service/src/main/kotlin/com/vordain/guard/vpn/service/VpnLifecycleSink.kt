package com.vordain.guard.vpn.service

interface VpnLifecycleSink {
    fun onVpnStarted()
    fun onVpnStopped()
    fun onVpnRevoked()

    companion object {
        val NoOp = object : VpnLifecycleSink {
            override fun onVpnStarted() = Unit
            override fun onVpnStopped() = Unit
            override fun onVpnRevoked() = Unit
        }
    }
}
