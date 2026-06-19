package com.vordain.guard.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object VpnForegroundNotification {
    const val NOTIFICATION_ID = 42_101
    const val CHANNEL_ID = "vordain_guard_vpn"
    const val CHANNEL_NAME = "Vordain Guard protection"
    const val TITLE = "Vordain Guard VPN shell active"
    const val BODY = "Protection service is running for setup testing"

    fun build(
        context: Context,
        mode: VpnForegroundNotificationMode = VpnForegroundNotificationMode.SHELL,
    ): Notification {
        ensureChannel(context)
        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(mode.title)
            .setContentText(mode.body)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationManager.createNotificationChannel(channel)
    }
}

enum class VpnForegroundNotificationMode(
    val title: String,
    val body: String,
) {
    SHELL(
        title = VpnForegroundNotification.TITLE,
        body = VpnForegroundNotification.BODY,
    ),
    BASIC_DNS_GUARD(
        title = "Vordain Guard DNS mode",
        body = "DNS-only enforcement is running. Not full protection.",
    ),
    DNS_ONLY_LAB(
        title = "Vordain DNS lab",
        body = "Local DNS lab is running. Not full protection.",
    ),
    FULL_TUNNEL_LAB(
        title = "Vordain full-tunnel lab",
        body = "Traffic may stop. Lab mode only.",
    ),
}
