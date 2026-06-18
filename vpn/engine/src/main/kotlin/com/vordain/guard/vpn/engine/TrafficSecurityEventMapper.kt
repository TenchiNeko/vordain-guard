package com.vordain.guard.vpn.engine

import com.vordain.guard.core.events.EventSeverity
import com.vordain.guard.core.events.SecurityEvent
import com.vordain.guard.core.events.SecurityEventFactory
import com.vordain.guard.core.events.SecurityEventType
import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.policy.PolicyDecisionReason

class TrafficSecurityEventMapper(
    private val securityEventFactory: SecurityEventFactory,
) {
    fun eventForDomainDecision(
        deviceId: DeviceId,
        domain: DomainName,
        trafficDecision: TrafficDecision,
    ): SecurityEvent? {
        if (!trafficDecision.evaluation.shouldCreateEvent) {
            return null
        }

        val eventType = eventTypeFor(trafficDecision.evaluation.reason)
        val severity = severityFor(eventType)
        return securityEventFactory.create(
            deviceId = deviceId,
            type = eventType,
            severity = severity,
            summary = summaryFor(eventType, domain),
        )
    }

    private fun eventTypeFor(reason: PolicyDecisionReason): SecurityEventType {
        return when (reason) {
            PolicyDecisionReason.PROXY_CATEGORY_BLOCKED -> SecurityEventType.PROXY_DOMAIN_BLOCKED
            PolicyDecisionReason.UNKNOWN_DOMAIN_BLOCKED -> SecurityEventType.UNKNOWN_DOMAIN_BLOCKED
            PolicyDecisionReason.BLOCKLIST_MATCH -> SecurityEventType.BLOCKED_DOMAIN
            else -> SecurityEventType.UNAPPROVED_APP_NETWORK_ATTEMPT
        }
    }

    private fun severityFor(eventType: SecurityEventType): EventSeverity {
        return when (eventType) {
            SecurityEventType.PROXY_DOMAIN_BLOCKED -> EventSeverity.HIGH
            SecurityEventType.UNKNOWN_DOMAIN_BLOCKED -> EventSeverity.MEDIUM
            SecurityEventType.BLOCKED_DOMAIN -> EventSeverity.MEDIUM
            SecurityEventType.UNAPPROVED_APP_NETWORK_ATTEMPT -> EventSeverity.HIGH
            SecurityEventType.VPN_STOPPED -> EventSeverity.CRITICAL
            SecurityEventType.VPN_RESTARTED -> EventSeverity.INFO
            SecurityEventType.POLICY_UPDATED -> EventSeverity.INFO
            SecurityEventType.CHILD_DEVICE_OFFLINE -> EventSeverity.HIGH
            SecurityEventType.TAMPER_SUSPECTED -> EventSeverity.CRITICAL
        }
    }

    private fun summaryFor(eventType: SecurityEventType, domain: DomainName): String {
        return when (eventType) {
            SecurityEventType.PROXY_DOMAIN_BLOCKED -> "Blocked proxy or anonymizer domain: ${domain.value}"
            SecurityEventType.UNKNOWN_DOMAIN_BLOCKED -> "Blocked unknown domain: ${domain.value}"
            SecurityEventType.BLOCKED_DOMAIN -> "Blocked domain: ${domain.value}"
            SecurityEventType.UNAPPROVED_APP_NETWORK_ATTEMPT -> "Unapproved network attempt: ${domain.value}"
            SecurityEventType.VPN_STOPPED -> "VPN protection stopped"
            SecurityEventType.VPN_RESTARTED -> "VPN protection restarted"
            SecurityEventType.POLICY_UPDATED -> "Policy updated"
            SecurityEventType.CHILD_DEVICE_OFFLINE -> "Child device appears offline"
            SecurityEventType.TAMPER_SUSPECTED -> "Tamper suspected"
        }
    }
}
