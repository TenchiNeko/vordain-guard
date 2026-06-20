package com.vordain.guard.vpn.session

enum class VpnRuntimeSessionState {
    IDLE,
    STARTING,
    ESTABLISHED,
    RUNNING,
    STOPPED,
    REVOKED,
    ERROR,
    STALE,
    UNKNOWN,
}

data class VpnRuntimeSnapshot(
    val operatingMode: VordainOperatingMode = VordainOperatingMode.IDLE,
    val sessionState: VpnRuntimeSessionState = VpnRuntimeSessionState.UNKNOWN,
    val updatedAtMillis: Long = 0L,
    val startedAtMillis: Long? = null,
    val stoppedAtMillis: Long? = null,
    val heartbeatLastTickAtMillis: Long? = null,
    val heartbeatStatus: BasicDnsGuardHeartbeatStatus = BasicDnsGuardHeartbeatStatus.MISSING,
    val activePolicyVersion: String? = null,
    val lastErrorCode: String? = null,
    val lastErrorMessage: String? = null,
    val descriptorEstablished: Boolean = false,
    val dnsLoopRunning: Boolean = false,
) {
    val statusLabel: String
        get() = when (sessionState) {
            VpnRuntimeSessionState.IDLE -> "Idle"
            VpnRuntimeSessionState.STARTING -> "Starting"
            VpnRuntimeSessionState.ESTABLISHED -> "VPN descriptor established"
            VpnRuntimeSessionState.RUNNING -> "Running"
            VpnRuntimeSessionState.STOPPED -> "Stopped"
            VpnRuntimeSessionState.REVOKED -> "Revoked"
            VpnRuntimeSessionState.ERROR -> "Needs attention"
            VpnRuntimeSessionState.STALE -> "Unknown"
            VpnRuntimeSessionState.UNKNOWN -> "Unknown"
        }

    companion object {
        fun unknown(currentTimeMillis: Long = 0L): VpnRuntimeSnapshot {
            return VpnRuntimeSnapshot(
                updatedAtMillis = currentTimeMillis,
                sessionState = VpnRuntimeSessionState.UNKNOWN,
            )
        }
    }
}

class VpnRuntimeSnapshotEvaluator {
    fun evaluateFreshness(
        snapshot: VpnRuntimeSnapshot,
        currentTimeMillis: Long,
        staleAfterMillis: Long = DEFAULT_STALE_AFTER_MILLIS,
    ): VpnRuntimeSnapshot {
        if (snapshot.updatedAtMillis <= 0L) {
            return VpnRuntimeSnapshot.unknown(currentTimeMillis)
        }
        if (snapshot.sessionState == VpnRuntimeSessionState.RUNNING &&
            currentTimeMillis - snapshot.updatedAtMillis > staleAfterMillis
        ) {
            return snapshot.copy(
                sessionState = VpnRuntimeSessionState.STALE,
                updatedAtMillis = currentTimeMillis,
                lastErrorCode = "STALE_RUNTIME",
                lastErrorMessage = "VPN runtime status is stale",
            )
        }
        return snapshot
    }

    companion object {
        const val DEFAULT_STALE_AFTER_MILLIS = 60_000L
    }
}
