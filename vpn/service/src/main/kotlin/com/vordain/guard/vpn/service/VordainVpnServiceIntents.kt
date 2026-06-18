package com.vordain.guard.vpn.service

import android.content.Context
import android.content.Intent

object VordainVpnServiceIntents {
    fun startProtection(context: Context): Intent {
        return Intent(context, VordainVpnService::class.java)
            .setAction(VordainVpnServiceActions.ACTION_START_PROTECTION)
    }

    fun stopProtection(context: Context): Intent {
        return Intent(context, VordainVpnService::class.java)
            .setAction(VordainVpnServiceActions.ACTION_STOP_PROTECTION)
    }
}
