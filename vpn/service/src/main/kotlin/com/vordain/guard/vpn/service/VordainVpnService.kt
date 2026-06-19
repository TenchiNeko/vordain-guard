package com.vordain.guard.vpn.service

import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import com.vordain.guard.vpn.session.VpnTunnelSpec

class VordainVpnService : VpnService() {
    private var tunnelHandle: AndroidVpnTunnelHandle? = null
    private var captureLoop: AndroidTunPacketCaptureLoop? = null

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
                establishTunnel(VpnTunnelSpec.establishOnlySmokeTest(), capturePackets = false)
                START_STICKY
            }
            VpnServiceCommandResult.HandledLabStart -> {
                startForeground(
                    VpnForegroundNotification.NOTIFICATION_ID,
                    VpnForegroundNotification.build(this),
                )
                establishTunnel(VpnTunnelSpec.labFullTunnelCapture(), capturePackets = true)
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
     * lifecycle callbacks and tunnel setup/teardown only.
     *
     * It must not contain blocklists, allowlists, lockdown rules, classifier
     * decisions, DNS parsing, network calls, or packet forwarding. The lab
     * capture path reads local TUN packets and drops them for developer smoke
     * testing only. Traffic evaluation belongs in pure Kotlin vpn-engine;
     * policy decisions belong in core/policy.
     */

    private fun establishTunnel(
        spec: VpnTunnelSpec,
        capturePackets: Boolean,
    ) {
        when (val result = tunnelOpener.establish(this, spec)) {
            is AndroidVpnTunnelOpenResult.Established -> {
                closeTunnel()
                tunnelHandle = result.handle
                if (capturePackets) {
                    captureLoop = AndroidTunPacketCaptureLoop(
                        descriptor = result.handle.descriptor,
                        sink = LabPacketCaptureDebugStatus.sink(),
                    ).also(AndroidTunPacketCaptureLoop::start)
                }
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
        captureLoop?.stop()
        captureLoop = null
        tunnelHandle?.close()
        tunnelHandle = null
    }

    companion object {
        var lifecycleSink: VpnLifecycleSink = VpnLifecycleSink.NoOp
        var sessionSink: VpnSessionSink = DefaultServiceVpnSessionSinkFactory.create()
        var tunnelOpener: AndroidVpnTunnelOpener = AndroidVpnTunnelOpener()
    }
}
