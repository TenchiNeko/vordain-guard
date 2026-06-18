package com.vordain.guard.vpn.lifecycle

import com.vordain.guard.core.events.EventSeverity
import com.vordain.guard.core.events.SecurityEvent
import com.vordain.guard.core.events.SecurityEventFactory
import com.vordain.guard.core.events.SecurityEventInput
import com.vordain.guard.core.events.SecurityEventType
import com.vordain.guard.data.local.SecurityEventQueue

class DefaultVpnLifecycleIncidentHandler(
    private val securityEventFactory: SecurityEventFactory,
    private val securityEventQueue: SecurityEventQueue,
) : VpnLifecycleIncidentHandler {
    override fun handle(incident: VpnLifecycleIncident): VpnLifecycleIncidentResult {
        val event = securityEventFactory.create(incident.toSecurityEventInput())
        securityEventQueue.enqueue(event)
        return VpnLifecycleIncidentResult(
            eventCreated = true,
            queued = true,
            eventId = event.id,
        )
    }

    private fun VpnLifecycleIncident.toSecurityEventInput(): SecurityEventInput {
        val eventShape = eventShape()
        return SecurityEventInput(
            deviceId = deviceId,
            type = eventShape.type,
            severity = eventShape.severity,
            summary = summary ?: eventShape.defaultSummary,
            createdAtMillis = occurredAtMillis,
        )
    }

    private fun VpnLifecycleIncident.eventShape(): EventShape {
        return when (type) {
            VpnLifecycleIncidentType.VPN_REVOKED -> EventShape(
                type = SecurityEventType.VPN_STOPPED,
                severity = EventSeverity.CRITICAL,
                defaultSummary = "VPN protection was revoked on the child device.",
            )
            VpnLifecycleIncidentType.VPN_STOPPED -> EventShape(
                type = SecurityEventType.VPN_STOPPED,
                severity = EventSeverity.CRITICAL,
                defaultSummary = "VPN protection stopped on the child device.",
            )
            VpnLifecycleIncidentType.VPN_STARTED -> EventShape(
                type = SecurityEventType.VPN_RESTARTED,
                severity = EventSeverity.INFO,
                defaultSummary = "VPN protection started on the child device.",
            )
        }
    }

    private data class EventShape(
        val type: SecurityEventType,
        val severity: EventSeverity,
        val defaultSummary: String,
    )
}
