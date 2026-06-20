package com.vordain.guard.vpn.service

import com.vordain.guard.vpn.session.BasicDnsGuardHeartbeatStatus
import com.vordain.guard.vpn.session.VordainOperatingMode
import com.vordain.guard.vpn.session.VpnRuntimeSessionState
import com.vordain.guard.vpn.session.VpnRuntimeSnapshot
import com.vordain.guard.vpn.session.VpnRuntimeSnapshotEvaluator

object VpnRuntimeDebugStatus {
    private val evaluator = VpnRuntimeSnapshotEvaluator()

    @Volatile
    private var latestSnapshot: VpnRuntimeSnapshot = VpnRuntimeSnapshot.unknown()

    @Synchronized
    fun markStarting(
        mode: VordainOperatingMode,
        currentTimeMillis: Long = System.currentTimeMillis(),
    ) {
        latestSnapshot = VpnRuntimeSnapshot(
            operatingMode = mode,
            sessionState = VpnRuntimeSessionState.STARTING,
            updatedAtMillis = currentTimeMillis,
            startedAtMillis = currentTimeMillis,
        )
    }

    @Synchronized
    fun markRunning(
        mode: VordainOperatingMode,
        descriptorEstablished: Boolean,
        dnsLoopRunning: Boolean,
        currentTimeMillis: Long = System.currentTimeMillis(),
    ) {
        latestSnapshot = latestSnapshot.copy(
            operatingMode = mode,
            sessionState = VpnRuntimeSessionState.RUNNING,
            updatedAtMillis = currentTimeMillis,
            startedAtMillis = latestSnapshot.startedAtMillis ?: currentTimeMillis,
            stoppedAtMillis = null,
            descriptorEstablished = descriptorEstablished,
            dnsLoopRunning = dnsLoopRunning,
            lastErrorCode = null,
            lastErrorMessage = null,
        )
    }

    @Synchronized
    fun markStopped(
        reason: String,
        currentTimeMillis: Long = System.currentTimeMillis(),
    ) {
        latestSnapshot = latestSnapshot.copy(
            sessionState = VpnRuntimeSessionState.STOPPED,
            updatedAtMillis = currentTimeMillis,
            stoppedAtMillis = currentTimeMillis,
            descriptorEstablished = false,
            dnsLoopRunning = false,
            lastErrorCode = null,
            lastErrorMessage = reason,
        )
    }

    @Synchronized
    fun markRevoked(currentTimeMillis: Long = System.currentTimeMillis()) {
        latestSnapshot = latestSnapshot.copy(
            sessionState = VpnRuntimeSessionState.REVOKED,
            updatedAtMillis = currentTimeMillis,
            stoppedAtMillis = currentTimeMillis,
            descriptorEstablished = false,
            dnsLoopRunning = false,
            lastErrorCode = "VPN_REVOKED",
            lastErrorMessage = "VPN permission was revoked",
        )
    }

    @Synchronized
    fun markError(
        code: String,
        message: String,
        currentTimeMillis: Long = System.currentTimeMillis(),
    ) {
        latestSnapshot = latestSnapshot.copy(
            sessionState = VpnRuntimeSessionState.ERROR,
            updatedAtMillis = currentTimeMillis,
            stoppedAtMillis = currentTimeMillis,
            descriptorEstablished = false,
            dnsLoopRunning = false,
            lastErrorCode = code,
            lastErrorMessage = message,
        )
    }

    @Synchronized
    fun updateHeartbeat(
        status: BasicDnsGuardHeartbeatStatus,
        lastTickAtMillis: Long?,
        currentTimeMillis: Long = System.currentTimeMillis(),
    ) {
        latestSnapshot = latestSnapshot.copy(
            updatedAtMillis = currentTimeMillis,
            heartbeatStatus = status,
            heartbeatLastTickAtMillis = lastTickAtMillis,
        )
    }

    fun snapshot(currentTimeMillis: Long = System.currentTimeMillis()): VpnRuntimeSnapshot {
        return evaluator.evaluateFreshness(latestSnapshot, currentTimeMillis)
    }
}
