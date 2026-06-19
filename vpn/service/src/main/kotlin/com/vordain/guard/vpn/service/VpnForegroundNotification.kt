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

    fun build(context: Context): Notification {
        ensureChannel(context)
        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(TITLE)
            .setContentText(BODY)
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
