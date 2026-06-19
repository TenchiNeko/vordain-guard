package com.vordain.guard.parent

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64

data class ParentReportHistoryEntry(
    val id: String,
    val type: String,
    val createdAtMillis: Long,
    val title: String,
    val payload: String,
)

class ParentReportHistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<ParentReportHistoryEntry> {
        return preferences.getString(KEY_ENTRIES, null)
            ?.lineSequence()
            ?.mapNotNull(::decodeEntry)
            ?.toList()
            ?: emptyList()
    }

    fun save(entries: List<ParentReportHistoryEntry>) {
        preferences.edit()
            .putString(KEY_ENTRIES, entries.take(MAX_ENTRIES).joinToString(separator = "\n", transform = ::encodeEntry))
            .apply()
    }

    private fun encodeEntry(entry: ParentReportHistoryEntry): String {
        return listOf(
            encode(entry.id),
            encode(entry.type),
            entry.createdAtMillis.toString(),
            encode(entry.title),
            encode(entry.payload),
        ).joinToString(separator = "|")
    }

    private fun decodeEntry(line: String): ParentReportHistoryEntry? {
        val parts = line.split('|')
        if (parts.size != FIELD_COUNT) {
            return null
        }
        return runCatching {
            ParentReportHistoryEntry(
                id = decode(parts[0]),
                type = decode(parts[1]),
                createdAtMillis = parts[2].toLong(),
                title = decode(parts[3]),
                payload = decode(parts[4]),
            )
        }.getOrNull()
    }

    private fun encode(value: String): String {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decode(value: String): String {
        return String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    }

    companion object {
        private const val PREFERENCES_NAME = "vordain_parent_report_history"
        private const val KEY_ENTRIES = "entries"
        private const val FIELD_COUNT = 5
        const val MAX_ENTRIES = 20
    }
}
