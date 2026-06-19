package com.vordain.guard.vpn.service

import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import com.vordain.guard.vpn.session.VpnTunnelSpec

class VordainVpnService : VpnService() {
    private var tunnelHandle: AndroidVpnTunnelHandle? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = VpnServiceCommandBridge(sessionSink).handleAction(intent?.action)
        return when (result) {
            VpnServiceCommandResult.HandledStart -> {
                startForeground(
                    VpnForegroundNotification.NOTIFICATION_ID,
                    VpnForegroundNotification.build(this),
                )
                establishTunnel()
                START_STICKY
            }
            VpnServiceCommandResult.HandledStop -> {
                closeTunnel()
                sessionSink.onVpnStopped()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
                START_NOT_STICKY
            }
            VpnServiceCommandResult.Ignored -> START_NOT_STICKY
        }
    }

    override fun onRevoke() {
        closeTunnel()
        sessionSink.onVpnRevoked()
        lifecycleSink.onVpnRevoked()
        super.onRevoke()
    }

    override fun onDestroy() {
        closeTunnel()
        sessionSink.onVpnStopped()
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

    private fun establishTunnel() {
        when (val result = tunnelOpener.establish(this, VpnTunnelSpec.establishOnlySmokeTest())) {
            is AndroidVpnTunnelOpenResult.Established -> {
                tunnelHandle?.close()
                tunnelHandle = result.handle
                sessionSink.onVpnStarted()
                lifecycleSink.onVpnStarted()
            }
            is AndroidVpnTunnelOpenResult.PermissionRequired -> {
                sessionSink.onVpnError(result.message ?: "VPN permission is required")
            }
            is AndroidVpnTunnelOpenResult.Failed -> {
                sessionSink.onVpnError(result.message ?: "VPN shell could not be established")
            }
        }
    }

    private fun closeTunnel() {
        tunnelHandle?.close()
        tunnelHandle = null
    }

    companion object {
        var lifecycleSink: VpnLifecycleSink = VpnLifecycleSink.NoOp
        var sessionSink: VpnSessionSink = DefaultServiceVpnSessionSinkFactory.create()
        var tunnelOpener: AndroidVpnTunnelOpener = AndroidVpnTunnelOpener()
    }
}
