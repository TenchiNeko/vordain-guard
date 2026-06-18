package com.vordain.guard.core.status

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.ProtectionState

class ProtectionStateEvaluator {
    fun evaluate(
        deviceId: DeviceId,
        latestReport: ProtectionReport?,
        currentTimeMillis: Long,
        heartbeatTimeoutMillis: Long,
    ): ProtectionEvaluation {
        require(heartbeatTimeoutMillis >= 0L) { "heartbeatTimeoutMillis must be non-negative" }

        if (latestReport == null) {
            return unknown(
                deviceId = deviceId,
                reason = ProtectionStatusReason.HEARTBEAT_MISSING,
                summary = "Protection status is unknown because no child device report is available.",
            )
        }

        val heartbeatAgeMillis = currentTimeMillis - latestReport.reportedAtMillis
        if (heartbeatAgeMillis > heartbeatTimeoutMillis) {
            return unknown(
                deviceId = deviceId,
                reason = ProtectionStatusReason.HEARTBEAT_STALE,
                summary = "Protection status is unknown because the child device has not reported recently.",
            )
        }

        val baseReasons = linkedSetOf(ProtectionStatusReason.HEARTBEAT_FRESH)
        if (latestReport.vpnActive) {
            baseReasons += ProtectionStatusReason.VPN_ACTIVE
        } else {
            baseReasons += ProtectionStatusReason.VPN_STOPPED
        }
        if (latestReport.policyLoaded) {
            baseReasons += ProtectionStatusReason.POLICY_LOADED
        } else {
            baseReasons += ProtectionStatusReason.POLICY_MISSING
        }

        if (!latestReport.vpnActive || latestReport.localTamperDetected) {
            if (latestReport.localTamperDetected) {
                baseReasons += ProtectionStatusReason.LOCAL_TAMPER_DETECTED
            }
            return ProtectionEvaluation(
                deviceId = latestReport.deviceId,
                state = ProtectionState.STOPPED,
                reasons = baseReasons,
                shouldAlertParent = true,
                summary = "Protection has stopped or local tamper was detected.",
            )
        }

        val degradedReasons = linkedSetOf<ProtectionStatusReason>()
        if (!latestReport.policyLoaded) {
            degradedReasons += ProtectionStatusReason.POLICY_MISSING
        }
        if (!latestReport.alwaysOnVpnEnabled) {
            degradedReasons += ProtectionStatusReason.ALWAYS_ON_VPN_DISABLED
        }
        if (!latestReport.blockWithoutVpnEnabled) {
            degradedReasons += ProtectionStatusReason.BLOCK_WITHOUT_VPN_DISABLED
        }
        if (!latestReport.appProtectionEnabled) {
            degradedReasons += ProtectionStatusReason.APP_PROTECTION_DISABLED
        }

        if (degradedReasons.isNotEmpty()) {
            return ProtectionEvaluation(
                deviceId = latestReport.deviceId,
                state = ProtectionState.DEGRADED,
                reasons = baseReasons + degradedReasons,
                shouldAlertParent = true,
                summary = "Protection is active but setup is incomplete or weakened.",
            )
        }

        return ProtectionEvaluation(
            deviceId = latestReport.deviceId,
            state = ProtectionState.PROTECTED,
            reasons = baseReasons,
            shouldAlertParent = false,
            summary = "Protection is currently confirmed.",
        )
    }

    private fun unknown(
        deviceId: DeviceId,
        reason: ProtectionStatusReason,
        summary: String,
    ): ProtectionEvaluation {
        return ProtectionEvaluation(
            deviceId = deviceId,
            state = ProtectionState.UNKNOWN,
            reasons = setOf(reason),
            shouldAlertParent = true,
            summary = summary,
        )
    }
}
