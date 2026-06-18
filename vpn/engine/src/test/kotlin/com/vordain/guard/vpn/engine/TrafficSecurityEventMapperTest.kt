package com.vordain.guard.vpn.engine

import com.vordain.guard.core.events.EventClock
import com.vordain.guard.core.events.EventIdProvider
import com.vordain.guard.core.events.EventSeverity
import com.vordain.guard.core.events.SecurityEventFactory
import com.vordain.guard.core.events.SecurityEventType
import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.policy.PolicyDecision
import com.vordain.guard.core.policy.PolicyDecisionReason
import com.vordain.guard.core.policy.PolicyEvaluation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrafficSecurityEventMapperTest {
    @Test
    fun proxyCategoryBlockMapsToProxyDomainBlockedHighSeverity() {
        val event = mapper().eventForDomainDecision(
            deviceId = DeviceId("child-device"),
            domain = DomainName.from("proxy.example"),
            trafficDecision = trafficDecision(
                action = TrafficAction.BLOCK,
                decision = PolicyDecision.Block,
                reason = PolicyDecisionReason.PROXY_CATEGORY_BLOCKED,
                shouldCreateEvent = true,
            ),
        )

        assertEquals(SecurityEventType.PROXY_DOMAIN_BLOCKED, event?.type)
        assertEquals(EventSeverity.HIGH, event?.severity)
        assertEquals("Blocked proxy or anonymizer domain: proxy.example", event?.summary)
    }

    @Test
    fun unknownDomainBlockMapsToUnknownDomainBlockedMediumSeverity() {
        val event = mapper().eventForDomainDecision(
            deviceId = DeviceId("child-device"),
            domain = DomainName.from("unknown.example"),
            trafficDecision = trafficDecision(
                action = TrafficAction.BLOCK,
                decision = PolicyDecision.Block,
                reason = PolicyDecisionReason.UNKNOWN_DOMAIN_BLOCKED,
                shouldCreateEvent = true,
            ),
        )

        assertEquals(SecurityEventType.UNKNOWN_DOMAIN_BLOCKED, event?.type)
        assertEquals(EventSeverity.MEDIUM, event?.severity)
    }

    @Test
    fun blocklistMatchMapsToBlockedDomainMediumSeverity() {
        val event = mapper().eventForDomainDecision(
            deviceId = DeviceId("child-device"),
            domain = DomainName.from("blocked.example"),
            trafficDecision = trafficDecision(
                action = TrafficAction.BLOCK,
                decision = PolicyDecision.Block,
                reason = PolicyDecisionReason.BLOCKLIST_MATCH,
                shouldCreateEvent = true,
            ),
        )

        assertEquals(SecurityEventType.BLOCKED_DOMAIN, event?.type)
        assertEquals(EventSeverity.MEDIUM, event?.severity)
        assertEquals("Blocked domain: blocked.example", event?.summary)
    }

    @Test
    fun allowDecisionWithoutEventDoesNotCreateEvent() {
        val event = mapper().eventForDomainDecision(
            deviceId = DeviceId("child-device"),
            domain = DomainName.from("school.example"),
            trafficDecision = trafficDecision(
                action = TrafficAction.ALLOW,
                decision = PolicyDecision.Allow,
                reason = PolicyDecisionReason.ALLOWLIST_MATCH,
                shouldCreateEvent = false,
            ),
        )

        assertNull(event)
    }

    @Test
    fun alertOnlyDecisionWithCreateEventCreatesEvent() {
        val event = mapper().eventForDomainDecision(
            deviceId = DeviceId("child-device"),
            domain = DomainName.from("unknown.example"),
            trafficDecision = trafficDecision(
                action = TrafficAction.ALERT_ONLY,
                decision = PolicyDecision.AlertOnly,
                reason = PolicyDecisionReason.NO_MATCH,
                shouldCreateEvent = true,
            ),
        )

        assertEquals(SecurityEventType.UNAPPROVED_APP_NETWORK_ATTEMPT, event?.type)
        assertEquals(EventSeverity.HIGH, event?.severity)
    }

    @Test
    fun blockDecisionWithCreateEventCreatesEvent() {
        val event = mapper().eventForDomainDecision(
            deviceId = DeviceId("child-device"),
            domain = DomainName.from("blocked.example"),
            trafficDecision = trafficDecision(
                action = TrafficAction.BLOCK,
                decision = PolicyDecision.Block,
                reason = PolicyDecisionReason.BLOCKLIST_MATCH,
                shouldCreateEvent = true,
            ),
        )

        assertEquals("event-1", event?.id)
        assertEquals(DeviceId("child-device"), event?.deviceId)
        assertEquals(1234L, event?.createdAtMillis)
    }

    @Test
    fun mappedSummaryIncludesOnlySingleDomain() {
        val event = mapper().eventForDomainDecision(
            deviceId = DeviceId("child-device"),
            domain = DomainName.from("one.example"),
            trafficDecision = trafficDecision(
                action = TrafficAction.BLOCK,
                decision = PolicyDecision.Block,
                reason = PolicyDecisionReason.BLOCKLIST_MATCH,
                shouldCreateEvent = true,
            ),
        )

        assertTrue(event?.summary?.contains("one.example") == true)
    }

    private fun mapper(): TrafficSecurityEventMapper {
        return TrafficSecurityEventMapper(
            SecurityEventFactory(
                eventIdProvider = StaticEventIdProvider("event-1"),
                eventClock = StaticEventClock(1234L),
            ),
        )
    }

    private fun trafficDecision(
        action: TrafficAction,
        decision: PolicyDecision,
        reason: PolicyDecisionReason,
        shouldCreateEvent: Boolean,
    ): TrafficDecision {
        return TrafficDecision(
            action = action,
            evaluation = PolicyEvaluation(
                decision = decision,
                reason = reason,
                shouldCreateEvent = shouldCreateEvent,
            ),
        )
    }

    private class StaticEventIdProvider(private val id: String) : EventIdProvider {
        override fun nextId(): String = id
    }

    private class StaticEventClock(private val nowMillis: Long) : EventClock {
        override fun nowMillis(): Long = nowMillis
    }
}
