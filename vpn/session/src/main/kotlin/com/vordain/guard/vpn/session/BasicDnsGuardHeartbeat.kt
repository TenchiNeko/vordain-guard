package com.vordain.guard.vpn.session

enum class BasicDnsGuardHeartbeatStatus {
    FRESH,
    STALE,
    MISSING,
    STOPPED,
}

data class BasicDnsGuardHeartbeatSnapshot(
    val mode: VordainOperatingMode,
    val startedAtMillis: Long?,
    val lastTickAtMillis: Long?,
    val tickCount: Long,
    val staleAfterMillis: Long,
    val status: BasicDnsGuardHeartbeatStatus,
    val statusLabel: String,
) {
    companion object {
        fun missing(staleAfterMillis: Long = DEFAULT_STALE_AFTER_MILLIS): BasicDnsGuardHeartbeatSnapshot {
            return BasicDnsGuardHeartbeatSnapshot(
                mode = VordainOperatingMode.IDLE,
                startedAtMillis = null,
                lastTickAtMillis = null,
                tickCount = 0,
                staleAfterMillis = staleAfterMillis,
                status = BasicDnsGuardHeartbeatStatus.MISSING,
                statusLabel = "Heartbeat missing",
            )
        }
    }
}

class BasicDnsGuardHeartbeat {
    fun start(
        mode: VordainOperatingMode,
        currentTimeMillis: Long,
        staleAfterMillis: Long = DEFAULT_STALE_AFTER_MILLIS,
    ): BasicDnsGuardHeartbeatSnapshot {
        return BasicDnsGuardHeartbeatSnapshot(
            mode = mode,
            startedAtMillis = currentTimeMillis,
            lastTickAtMillis = currentTimeMillis,
            tickCount = 1,
            staleAfterMillis = staleAfterMillis,
            status = BasicDnsGuardHeartbeatStatus.FRESH,
            statusLabel = "Heartbeat fresh",
        )
    }

    fun tick(
        snapshot: BasicDnsGuardHeartbeatSnapshot,
        currentTimeMillis: Long,
    ): BasicDnsGuardHeartbeatSnapshot {
        if (snapshot.status == BasicDnsGuardHeartbeatStatus.STOPPED) {
            return snapshot
        }
        return snapshot.copy(
            lastTickAtMillis = currentTimeMillis,
            tickCount = snapshot.tickCount + 1,
            status = BasicDnsGuardHeartbeatStatus.FRESH,
            statusLabel = "Heartbeat fresh",
        )
    }

    fun stop(
        snapshot: BasicDnsGuardHeartbeatSnapshot,
        reason: String,
    ): BasicDnsGuardHeartbeatSnapshot {
        return snapshot.copy(
            status = BasicDnsGuardHeartbeatStatus.STOPPED,
            statusLabel = reason.ifBlank { "Heartbeat stopped" },
        )
    }

    fun evaluate(
        snapshot: BasicDnsGuardHeartbeatSnapshot,
        currentTimeMillis: Long,
    ): BasicDnsGuardHeartbeatSnapshot {
        if (snapshot.status == BasicDnsGuardHeartbeatStatus.STOPPED) {
            return snapshot
        }
        val lastTick = snapshot.lastTickAtMillis
            ?: return snapshot.copy(
                status = BasicDnsGuardHeartbeatStatus.MISSING,
                statusLabel = "Heartbeat missing",
            )
        return if (currentTimeMillis - lastTick > snapshot.staleAfterMillis) {
            snapshot.copy(
                status = BasicDnsGuardHeartbeatStatus.STALE,
                statusLabel = "Heartbeat stale",
            )
        } else {
            snapshot.copy(
                status = BasicDnsGuardHeartbeatStatus.FRESH,
                statusLabel = "Heartbeat fresh",
            )
        }
    }

    fun isBasicDnsGuardHeartbeat(snapshot: BasicDnsGuardHeartbeatSnapshot): Boolean {
        return snapshot.mode == VordainOperatingMode.BASIC_DNS_GUARD &&
            snapshot.status != BasicDnsGuardHeartbeatStatus.STOPPED
    }
}

const val DEFAULT_STALE_AFTER_MILLIS: Long = 45_000L
