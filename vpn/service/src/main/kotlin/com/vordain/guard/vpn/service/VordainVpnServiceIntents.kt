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

    fun startDnsOnlyLab(context: Context): Intent {
        return Intent(context, VordainVpnService::class.java)
            .setAction(VordainVpnServiceActions.ACTION_START_DNS_ONLY_LAB)
    }

    fun stopDnsOnlyLab(context: Context): Intent {
        return Intent(context, VordainVpnService::class.java)
            .setAction(VordainVpnServiceActions.ACTION_STOP_DNS_ONLY_LAB)
    }

    fun startBasicDnsGuard(context: Context): Intent {
        return Intent(context, VordainVpnService::class.java)
            .setAction(VordainVpnServiceActions.ACTION_START_BASIC_DNS_GUARD)
    }

    fun stopBasicDnsGuard(context: Context): Intent {
        return Intent(context, VordainVpnService::class.java)
            .setAction(VordainVpnServiceActions.ACTION_STOP_BASIC_DNS_GUARD)
    }
}
