package com.vordain.guard.vpn.service

import android.content.Intent
import android.net.VpnService
import android.os.IBinder

class VordainVpnService : VpnService() {
    override fun onCreate() {
        super.onCreate()
        lifecycleSink.onVpnStarted()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onRevoke() {
        lifecycleSink.onVpnRevoked()
        super.onRevoke()
    }

    override fun onDestroy() {
        lifecycleSink.onVpnStopped()
        super.onDestroy()
    }

    /*
     * This class is only an Android platform adapter. It owns Android VPN
     * lifecycle callbacks and future tunnel setup/teardown only.
     *
     * It must not contain blocklists, allowlists, lockdown rules, classifier
     * decisions, DNS parsing, network calls, or packet I/O loops. Traffic
     * evaluation belongs in pure Kotlin vpn-engine; policy decisions belong in
     * core/policy.
     */

    companion object {
        var lifecycleSink: VpnLifecycleSink = VpnLifecycleSink.NoOp
    }
}
