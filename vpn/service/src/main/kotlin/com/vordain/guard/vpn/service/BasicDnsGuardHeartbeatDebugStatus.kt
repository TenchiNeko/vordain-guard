package com.vordain.guard.vpn.service

import com.vordain.guard.vpn.session.BasicDnsGuardHeartbeat
import com.vordain.guard.vpn.session.BasicDnsGuardHeartbeatSnapshot
import com.vordain.guard.vpn.session.VordainOperatingMode

object BasicDnsGuardHeartbeatDebugStatus {
    private val heartbeat = BasicDnsGuardHeartbeat()

    @Volatile
    private var snapshot: BasicDnsGuardHeartbeatSnapshot = BasicDnsGuardHeartbeatSnapshot.missing()

    fun start(
        mode: VordainOperatingMode,
        currentTimeMillis: Long,
    ): BasicDnsGuardHeartbeatSnapshot {
        snapshot = heartbeat.start(
            mode = mode,
            currentTimeMillis = currentTimeMillis,
        )
        return snapshot
    }

    fun tick(currentTimeMillis: Long): BasicDnsGuardHeartbeatSnapshot {
        snapshot = heartbeat.tick(snapshot, currentTimeMillis)
        return snapshot
    }

    fun stop(reason: String): BasicDnsGuardHeartbeatSnapshot {
        snapshot = heartbeat.stop(snapshot, reason)
        return snapshot
    }

    fun snapshot(currentTimeMillis: Long = System.currentTimeMillis()): BasicDnsGuardHeartbeatSnapshot {
        val evaluated = heartbeat.evaluate(snapshot, currentTimeMillis)
        snapshot = evaluated
        return evaluated
    }
}
