package com.vordain.guard.vpn.engine

import com.vordain.guard.core.model.AppTrafficMode
import com.vordain.guard.core.policy.Policy

interface AppAwareTrafficGate {
    fun evaluate(
        observation: TrafficObservation,
        policy: Policy,
        appMode: AppTrafficMode,
    ): AppAwareTrafficGateDecision
}
