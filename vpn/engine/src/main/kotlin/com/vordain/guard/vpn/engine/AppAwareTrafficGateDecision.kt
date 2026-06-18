package com.vordain.guard.vpn.engine

data class AppAwareTrafficGateDecision(
    val action: TrafficAction,
    val reason: AppAwareTrafficGateReason,
    val cacheTtlMillis: Long?,
    val shouldCreateEvent: Boolean,
    val trafficDecision: TrafficDecision?,
)
