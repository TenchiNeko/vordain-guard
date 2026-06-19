package com.vordain.guard.core.auditlog

import com.vordain.guard.core.model.DeviceId
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuditTimelineReducerTest {
    private val reducer = AuditTimelineReducer()

    @Test
    fun appendAddsEntryNewestFirst() {
        val timeline = reducer.append(AuditTimeline(), entry("1"), maxEntries = 10)

        assertEquals(listOf("1"), timeline.entries.map(AuditEntry::id))
    }

    @Test
    fun maxEntriesCapWorks() {
        val timeline = (1..5).fold(AuditTimeline()) { current, index ->
            reducer.append(current, entry(index.toString()), maxEntries = 3)
        }

        assertEquals(listOf("5", "4", "3"), timeline.entries.map(AuditEntry::id))
    }

    @Test
    fun latestReturnsExpectedOrder() {
        val timeline = (1..4).fold(AuditTimeline()) { current, index ->
            reducer.append(current, entry(index.toString()), maxEntries = 10)
        }

        assertEquals(listOf("4", "3"), reducer.latest(timeline, 2).map(AuditEntry::id))
    }

    @Test
    fun clearRemovesEntries() {
        val timeline = reducer.append(AuditTimeline(), entry("1"), maxEntries = 10)

        assertTrue(reducer.clear(timeline).entries.isEmpty())
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
    fun sourceHasNoAndroidImports() {
        val source = Files.walk(Path("src/main"))
            .filter(Files::isRegularFile)
            .map { Files.readString(it) }
            .toList()
            .joinToString(separator = "\n")

        assertFalse(source.contains("android" + "."))
    }

    @Test
    fun sourceHasNoManagerNamesOrTodos() {
        val source = Files.walk(Path("src/main"))
            .filter(Files::isRegularFile)
            .map { Files.readString(it) }
            .toList()
            .joinToString(separator = "\n")

        assertFalse(Regex("""\b(class|object)\s+\w*Manager\b""").containsMatchIn(source))
        assertFalse(source.contains("TO" + "DO"))
        assertFalse(source.contains("FIX" + "ME"))
    }

    private fun entry(id: String): AuditEntry {
        return AuditEntry(
            id = id,
            type = AuditEntryType.STATUS_REPORT_GENERATED,
            severity = AuditSeverity.INFO,
            occurredAtMillis = id.toLong(),
            title = "Entry $id",
            detail = "Local explicit app action",
            deviceId = DeviceId("child-debug-device"),
        )
    }
}
