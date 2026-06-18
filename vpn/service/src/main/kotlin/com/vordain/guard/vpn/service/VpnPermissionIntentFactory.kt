package com.vordain.guard.vpn.service

import android.content.Context
import android.content.Intent
import android.net.VpnService

class VpnPermissionIntentFactory {
    fun createPrepareResult(context: Context): VpnPrepareResult {
        val consentIntent = VpnService.prepare(context)
        return if (consentIntent == null) {
            VpnPrepareResult.AlreadyGranted
        } else {
            VpnPrepareResult.ConsentRequired(consentIntent)
        }
    }
}

sealed class VpnPrepareResult {
    data object AlreadyGranted : VpnPrepareResult()
    data class ConsentRequired(val intent: Intent) : VpnPrepareResult()
}
