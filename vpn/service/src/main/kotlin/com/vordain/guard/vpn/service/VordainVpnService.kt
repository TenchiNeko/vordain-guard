package com.vordain.guard.vpn.service

import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import com.vordain.guard.vpn.session.LabCaptureWatchdog
import com.vordain.guard.vpn.session.LabCaptureWatchdogConfig
import com.vordain.guard.vpn.session.LabCaptureWatchdogState
import com.vordain.guard.vpn.session.VordainOperatingMode
import com.vordain.guard.vpn.session.VpnTunnelSpec
import java.util.concurrent.atomic.AtomicBoolean

class VordainVpnService : VpnService() {
    private var tunnelHandle: AndroidVpnTunnelHandle? = null
    private var captureLoop: AndroidTunPacketCaptureLoop? = null
    private val labWatchdog = LabCaptureWatchdog()
    private val labWatchdogRunning = AtomicBoolean(false)
    @Volatile
    private var labWatchdogState: LabCaptureWatchdogState = LabCaptureWatchdogState.inactive()
    private var labWatchdogThread: Thread? = null
    private val basicDnsHeartbeatRunning = AtomicBoolean(false)
    private var basicDnsHeartbeatThread: Thread? = null

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
                    VpnForegroundNotification.build(this, VpnForegroundNotificationMode.SHELL),
                )
                establishTunnel(VpnTunnelSpec.establishOnlySmokeTest(), capturePackets = false)
                START_STICKY
            }
            VpnServiceCommandResult.HandledLabStart -> {
                startForeground(
                    VpnForegroundNotification.NOTIFICATION_ID,
                    VpnForegroundNotification.build(this, VpnForegroundNotificationMode.FULL_TUNNEL_LAB),
                )
                establishTunnel(
                    spec = VpnTunnelSpec.labFullTunnelCapture(),
                    capturePackets = true,
                    labCaptureMode = ServiceLabCaptureMode.FULL_TUNNEL,
                )
                START_STICKY
            }
            VpnServiceCommandResult.HandledDnsOnlyLabStart -> {
                startForeground(
                    VpnForegroundNotification.NOTIFICATION_ID,
                    VpnForegroundNotification.build(this, VpnForegroundNotificationMode.DNS_ONLY_LAB),
                )
                establishTunnel(
                    spec = VpnTunnelSpec.dnsOnlyLabFiltering(),
                    capturePackets = true,
                    labCaptureMode = ServiceLabCaptureMode.DNS_ONLY,
                )
                START_STICKY
            }
            VpnServiceCommandResult.HandledBasicDnsGuardStart -> {
                startForeground(
                    VpnForegroundNotification.NOTIFICATION_ID,
                    VpnForegroundNotification.build(this, VpnForegroundNotificationMode.BASIC_DNS_GUARD),
                )
                establishTunnel(
                    spec = VpnTunnelSpec.basicDnsGuard(),
                    capturePackets = true,
                    labCaptureMode = ServiceLabCaptureMode.BASIC_DNS_GUARD,
                )
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
        labCaptureMode: ServiceLabCaptureMode = ServiceLabCaptureMode.NONE,
    ) {
        when (val result = tunnelOpener.establish(this, spec)) {
            is AndroidVpnTunnelOpenResult.Established -> {
                closeTunnel()
                tunnelHandle = result.handle
                if (capturePackets) {
                    when (labCaptureMode) {
                        ServiceLabCaptureMode.FULL_TUNNEL -> {
                            LabCaptureDebugStatus.configureFullTunnelProtectedDnsUpstream(this)
                        }
                        ServiceLabCaptureMode.DNS_ONLY -> {
                            LabCaptureDebugStatus.configureDnsOnlyProtectedDnsUpstream(this)
                        }
                        ServiceLabCaptureMode.BASIC_DNS_GUARD -> {
                            LabCaptureDebugStatus.configureDnsOnlyProtectedDnsUpstream(this)
                        }
                        ServiceLabCaptureMode.NONE -> Unit
                    }
                    captureLoop = AndroidTunPacketCaptureLoop(
                        descriptor = result.handle.descriptor,
                        observer = LabCaptureDebugStatus.observer(),
                    ).also(AndroidTunPacketCaptureLoop::start)
                    startLabWatchdog(
                        config = if (labCaptureMode == ServiceLabCaptureMode.DNS_ONLY ||
                            labCaptureMode == ServiceLabCaptureMode.BASIC_DNS_GUARD
                        ) {
                            DNS_ONLY_LAB_WATCHDOG_CONFIG
                        } else {
                            FULL_TUNNEL_LAB_WATCHDOG_CONFIG
                        },
                        reason = when (labCaptureMode) {
                            ServiceLabCaptureMode.BASIC_DNS_GUARD -> "Basic DNS Guard auto-stop watchdog active"
                            ServiceLabCaptureMode.DNS_ONLY -> "DNS-only lab auto-stop watchdog active"
                            ServiceLabCaptureMode.FULL_TUNNEL -> "full-tunnel lab auto-stop watchdog active"
                            ServiceLabCaptureMode.NONE -> "lab auto-stop watchdog active"
                        },
                    )
                    startBasicDnsHeartbeat(labCaptureMode)
                } else {
                    stopLabWatchdog("normal shell established")
                    stopBasicDnsHeartbeat("Heartbeat stopped")
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
        stopBasicDnsHeartbeat("Heartbeat stopped")
        captureLoop?.stop()
        captureLoop = null
        tunnelHandle?.close()
        tunnelHandle = null
    }

    private fun startLabWatchdog(
        config: LabCaptureWatchdogConfig,
        reason: String,
    ) {
        val now = System.currentTimeMillis()
        labWatchdogState = labWatchdog.start(
            config = config,
            currentTimeMillis = now,
            reason = reason,
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

    private fun startBasicDnsHeartbeat(labCaptureMode: ServiceLabCaptureMode) {
        val mode = when (labCaptureMode) {
            ServiceLabCaptureMode.BASIC_DNS_GUARD -> VordainOperatingMode.BASIC_DNS_GUARD
            ServiceLabCaptureMode.DNS_ONLY -> VordainOperatingMode.DNS_ONLY_LAB
            else -> {
                stopBasicDnsHeartbeat("Heartbeat stopped")
                return
            }
        }
        BasicDnsGuardHeartbeatDebugStatus.start(
            mode = mode,
            currentTimeMillis = System.currentTimeMillis(),
        )
        if (!basicDnsHeartbeatRunning.compareAndSet(false, true)) {
            return
        }
        basicDnsHeartbeatThread = Thread({
            while (basicDnsHeartbeatRunning.get()) {
                try {
                    Thread.sleep(BASIC_DNS_HEARTBEAT_INTERVAL_MILLIS)
                } catch (_: InterruptedException) {
                    return@Thread
                }
                if (basicDnsHeartbeatRunning.get()) {
                    BasicDnsGuardHeartbeatDebugStatus.tick(System.currentTimeMillis())
                }
            }
        }, "VordainBasicDnsHeartbeat").apply {
            isDaemon = true
            start()
        }
    }

    private fun stopBasicDnsHeartbeat(reason: String) {
        if (basicDnsHeartbeatRunning.getAndSet(false)) {
            val thread = basicDnsHeartbeatThread
            if (thread != null && thread != Thread.currentThread()) {
                thread.interrupt()
            }
            basicDnsHeartbeatThread = null
        }
        BasicDnsGuardHeartbeatDebugStatus.stop(reason)
    }

    companion object {
        var lifecycleSink: VpnLifecycleSink = VpnLifecycleSink.NoOp
        var sessionSink: VpnSessionSink = DefaultServiceVpnSessionSinkFactory.create()
        var tunnelOpener: AndroidVpnTunnelOpener = AndroidVpnTunnelOpener()
        val FULL_TUNNEL_LAB_WATCHDOG_CONFIG = LabCaptureWatchdogConfig()
        val DNS_ONLY_LAB_WATCHDOG_CONFIG = LabCaptureWatchdogConfig(
            maxSessionMillis = 30L * 60L * 1_000L,
        )
        private const val BASIC_DNS_HEARTBEAT_INTERVAL_MILLIS = 15_000L
    }
}

private enum class ServiceLabCaptureMode {
    NONE,
    FULL_TUNNEL,
    DNS_ONLY,
    BASIC_DNS_GUARD,
}
