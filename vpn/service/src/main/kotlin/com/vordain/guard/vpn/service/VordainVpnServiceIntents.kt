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

    fun startLabCapture(context: Context): Intent {
        return Intent(context, VordainVpnService::class.java)
            .setAction(VordainVpnServiceActions.ACTION_START_LAB_CAPTURE)
    }

    fun stopLabCapture(context: Context): Intent {
        return Intent(context, VordainVpnService::class.java)
            .setAction(VordainVpnServiceActions.ACTION_STOP_LAB_CAPTURE)
    }
}
