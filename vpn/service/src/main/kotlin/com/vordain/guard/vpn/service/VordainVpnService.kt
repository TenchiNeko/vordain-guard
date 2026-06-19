package com.vordain.guard.vpn.service

import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import com.vordain.guard.vpn.session.LabCaptureWatchdog
import com.vordain.guard.vpn.session.LabCaptureWatchdogConfig
import com.vordain.guard.vpn.session.LabCaptureWatchdogState
import com.vordain.guard.vpn.session.VpnTunnelSpec
import java.util.concurrent.atomic.AtomicBoolean

class VordainVpnService : VpnService() {
    private var tunnelHandle: AndroidVpnTunnelHandle? = null
    private var captureLoop: AndroidTunPacketCaptureLoop? = null
    private val labWatchdog = LabCaptureWatchdog()
    private val labWatchdogConfig = LabCaptureWatchdogConfig()
    private val labWatchdogRunning = AtomicBoolean(false)
    @Volatile
    private var labWatchdogState: LabCaptureWatchdogState = LabCaptureWatchdogState.inactive()
    private var labWatchdogThread: Thread? = null

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
                    LabCaptureDebugStatus.configureProtectedDnsUpstream(this)
                    captureLoop = AndroidTunPacketCaptureLoop(
                        descriptor = result.handle.descriptor,
                        observer = LabCaptureDebugStatus.observer(),
                    ).also(AndroidTunPacketCaptureLoop::start)
                    startLabWatchdog()
                } else {
                    stopLabWatchdog("normal shell established")
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
        stopLabWatchdog("lab capture stopped")
        captureLoop?.stop()
        captureLoop = null
        tunnelHandle?.close()
        tunnelHandle = null
    }

    private fun startLabWatchdog() {
        val now = System.currentTimeMillis()
        labWatchdogState = labWatchdog.start(
            config = labWatchdogConfig,
            currentTimeMillis = now,
            reason = "lab DNS enforcement auto-stop watchdog active",
        )
        LabCaptureDebugStatus.updateWatchdogState(labWatchdogState)
        if (!labWatchdogState.active || !labWatchdogRunning.compareAndSet(false, true)) {
            return
        }
        val expiresAt = labWatchdogState.expiresAtMillis ?: return
        labWatchdogThread = Thread({
            val delayMillis = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
            try {
                Thread.sleep(delayMillis)
            } catch (_: InterruptedException) {
                return@Thread
            }
            if (labWatchdogRunning.get() && labWatchdog.isExpired(labWatchdogState, System.currentTimeMillis())) {
                closeTunnel()
                sessionSink.onVpnStopped()
                lifecycleSink.onVpnStopped()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }, "VordainLabWatchdog").apply {
            isDaemon = true
            start()
        }
    }

    private fun stopLabWatchdog(reason: String) {
        if (labWatchdogRunning.getAndSet(false)) {
            val thread = labWatchdogThread
            if (thread != null && thread != Thread.currentThread()) {
                thread.interrupt()
            }
            labWatchdogThread = null
        }
        labWatchdogState = labWatchdog.stop(reason)
        LabCaptureDebugStatus.updateWatchdogState(labWatchdogState)
    }

    companion object {
        var lifecycleSink: VpnLifecycleSink = VpnLifecycleSink.NoOp
        var sessionSink: VpnSessionSink = DefaultServiceVpnSessionSinkFactory.create()
        var tunnelOpener: AndroidVpnTunnelOpener = AndroidVpnTunnelOpener()
    }
}
