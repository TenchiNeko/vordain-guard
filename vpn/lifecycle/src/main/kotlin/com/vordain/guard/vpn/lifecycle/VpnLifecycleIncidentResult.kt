package com.vordain.guard.vpn.lifecycle

data class VpnLifecycleIncidentResult(
    val eventCreated: Boolean,
    val queued: Boolean,
    val eventId: String?,
)
