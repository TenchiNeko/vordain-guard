package com.vordain.guard.vpn.engine

interface NetworkStateMonitor {
    fun isNetworkAvailable(): Boolean
}
