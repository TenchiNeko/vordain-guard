package com.vordain.guard.core.status

import com.vordain.guard.core.events.EventSeverity
import com.vordain.guard.core.events.SecurityEvent
import com.vordain.guard.core.events.SecurityEventFactory
import com.vordain.guard.core.events.SecurityEventType
import com.vordain.guard.core.model.ProtectionState

class ProtectionEventFactory(
    private val securityEventFactory: SecurityEventFactory,
) {
    fun createEvent(evaluation: ProtectionEvaluation): SecurityEvent? {
        val eventType = eventTypeFor(evaluation) ?: return null
        return securityEventFactory.create(
            deviceId = evaluation.deviceId,
            type = eventType,
            severity = severityFor(eventType),
            summary = summaryFor(eventType),
        )
    }

    private fun eventTypeFor(evaluation: ProtectionEvaluation): SecurityEventType? {
        return when (evaluation.state) {
            ProtectionState.PROTECTED -> null
            ProtectionState.DEGRADED -> null
            ProtectionState.UNKNOWN -> SecurityEventType.CHILD_DEVICE_OFFLINE
            ProtectionState.STOPPED -> stoppedEventTypeFor(evaluation)
        }
    }

    private fun stoppedEventTypeFor(evaluation: ProtectionEvaluation): SecurityEventType {
        return if (ProtectionStatusReason.LOCAL_TAMPER_DETECTED in evaluation.reasons) {
            SecurityEventType.TAMPER_SUSPECTED
        } else {
            SecurityEventType.VPN_STOPPED
        }
    }

    private fun severityFor(eventType: SecurityEventType): EventSeverity {
        return when (eventType) {
            SecurityEventType.CHILD_DEVICE_OFFLINE -> EventSeverity.HIGH
            SecurityEventType.VPN_STOPPED -> EventSeverity.CRITICAL
            SecurityEventType.TAMPER_SUSPECTED -> EventSeverity.CRITICAL
            SecurityEventType.VPN_RESTARTED,
            SecurityEventType.POLICY_UPDATED,
            -> EventSeverity.INFO
            SecurityEventType.PROXY_DOMAIN_BLOCKED,
            SecurityEventType.UNAPPROVED_APP_NETWORK_ATTEMPT,
            -> EventSeverity.HIGH
            SecurityEventType.UNKNOWN_DOMAIN_BLOCKED,
            SecurityEventType.BLOCKED_DOMAIN,
            -> EventSeverity.MEDIUM
        }
    }

    private fun summaryFor(eventType: SecurityEventType): String {
        return when (eventType) {
            SecurityEventType.CHILD_DEVICE_OFFLINE -> "Protection is no longer confirmed because the child device stopped reporting."
            SecurityEventType.VPN_STOPPED -> "VPN protection was stopped or disabled."
            SecurityEventType.TAMPER_SUSPECTED -> "Local tamper was detected on the child device."
            SecurityEventType.VPN_RESTARTED -> "VPN protection restarted."
            SecurityEventType.POLICY_UPDATED -> "Policy updated."
            SecurityEventType.PROXY_DOMAIN_BLOCKED -> "Proxy or anonymizer domain was blocked."
            SecurityEventType.UNKNOWN_DOMAIN_BLOCKED -> "Unknown domain was blocked."
            SecurityEventType.BLOCKED_DOMAIN -> "Blocked domain attempted."
            SecurityEventType.UNAPPROVED_APP_NETWORK_ATTEMPT -> "Unapproved network attempt detected."
        }
    }
}
