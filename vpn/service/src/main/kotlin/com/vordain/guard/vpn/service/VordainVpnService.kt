package com.vordain.guard.vpn.service

import android.content.Intent
import android.net.VpnService
import android.os.IBinder

class VordainVpnService : VpnService() {
    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /*
     * This class is only an Android platform adapter. It should establish and
     * stop the VPN tunnel, then forward packet and lifecycle work to vpn-engine.
     * Blocklists, allowlists, lockdown rules, and classifier decisions do not
     * belong in this service.
     */
}
