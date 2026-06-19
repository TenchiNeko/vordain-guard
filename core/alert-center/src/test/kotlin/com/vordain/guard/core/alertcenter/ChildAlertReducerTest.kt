package com.vordain.guard.core.alertcenter

import com.vordain.guard.core.model.DeviceId
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChildAlertReducerTest {
    private val reducer = ChildAlertReducer()

    @Test
    fun appendAddsAlert() {
        val timeline = reducer.append(ChildAlertTimeline(), alert("1"), maxAlerts = 10)

        assertEquals(listOf("1"), timeline.alerts.map(ChildAlert::id))
    }

    @Test
    fun maxAlertsCapWorks() {
        val timeline = (1..5).fold(ChildAlertTimeline()) { current, index ->
            reducer.append(current, alert(index.toString()), maxAlerts = 3)
        }

        assertEquals(listOf("5", "4", "3"), timeline.alerts.map(ChildAlert::id))
    }

    @Test
    fun acknowledgeChangesStatus() {
        val timeline = reducer.append(ChildAlertTimeline(), alert("1"), maxAlerts = 10)

        val updated = reducer.acknowledge(timeline, "1", acknowledgedAtMillis = 2L)

        assertEquals(AlertStatus.ACKNOWLEDGED, updated.alerts.first().status)
    }

    @Test
    fun clearChangesStatus() {
        val timeline = reducer.append(ChildAlertTimeline(), alert("1"), maxAlerts = 10)

        val updated = reducer.clear(timeline, "1", clearedAtMillis = 2L)

        assertEquals(AlertStatus.CLEARED, updated.alerts.first().status)
    }

    @Test
    fun activeCriticalCountCountsActiveCriticalOnly() {
        val timeline = listOf(
            alert("1", severity = AlertSeverity.CRITICAL, status = AlertStatus.ACTIVE),
            alert("2", severity = AlertSeverity.CRITICAL, status = AlertStatus.ACKNOWLEDGED),
            alert("3", severity = AlertSeverity.HIGH, status = AlertStatus.ACTIVE),
        ).fold(ChildAlertTimeline()) { current, alert ->
            reducer.append(current, alert, maxAlerts = 10)
        }

        assertEquals(1, reducer.activeCriticalCount(timeline))
    }

    @Test
    fun latestReturnsExpectedOrder() {
        val timeline = (1..4).fold(ChildAlertTimeline()) { current, index ->
            reducer.append(current, alert(index.toString()), maxAlerts = 10)
        }

        assertEquals(listOf("4", "3"), reducer.latest(timeline, 2).map(ChildAlert::id))
    }

    @Test
    fun sourceHasNoForbiddenTerms() {
        val source = Files.walk(Path("src/main"))
            .filter(Files::isRegularFile)
            .map { Files.readString(it) }
            .toList()
            .joinToString(separator = "\n")

        listOf(
            "browsing history",
            "traffic log",
            "raw packet",
            "screenshot",
            "message content",
            "child activity",
            "passive telemetry",
            "relay payload",
        ).forEach { term ->
            assertFalse(source.contains(term, ignoreCase = true), term)
        }
    }

    @Test
    fun sourceHasNoAndroidImportsOrManagerNamesOrTodos() {
        val source = Files.walk(Path("src/main"))
            .filter(Files::isRegularFile)
            .map { Files.readString(it) }
            .toList()
            .joinToString(separator = "\n")

        assertFalse(source.contains("android" + "."))
        assertFalse(Regex("""\b(class|object)\s+\w*Manager\b""").containsMatchIn(source))
        assertFalse(source.contains("TO" + "DO"))
        assertFalse(source.contains("FIX" + "ME"))
    }

    private fun alert(
        id: String,
        severity: AlertSeverity = AlertSeverity.CRITICAL,
        status: AlertStatus = AlertStatus.ACTIVE,
    ): ChildAlert {
        return ChildAlert(
            id = id,
            type = AlertType.VPN_REVOKED,
            severity = severity,
            status = status,
            childDeviceId = DeviceId("child-debug-device"),
            occurredAtMillis = id.toLong(),
            title = "Alert $id",
            detail = "Security state event",
            sourceLabel = "test",
        )
    }
}
