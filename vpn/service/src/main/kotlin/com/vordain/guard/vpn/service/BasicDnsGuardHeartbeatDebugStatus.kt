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
    ) {
        snapshot = heartbeat.start(
            mode = mode,
            currentTimeMillis = currentTimeMillis,
        )
    }

    fun tick(currentTimeMillis: Long) {
        snapshot = heartbeat.tick(snapshot, currentTimeMillis)
    }

    fun stop(reason: String) {
        snapshot = heartbeat.stop(snapshot, reason)
    }

    fun snapshot(currentTimeMillis: Long = System.currentTimeMillis()): BasicDnsGuardHeartbeatSnapshot {
        val evaluated = heartbeat.evaluate(snapshot, currentTimeMillis)
        snapshot = evaluated
        return evaluated
    }
}
