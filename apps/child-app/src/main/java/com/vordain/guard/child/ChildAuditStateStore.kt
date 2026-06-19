package com.vordain.guard.child

import android.content.Context
import com.vordain.guard.core.auditlog.AuditEntry
import com.vordain.guard.core.auditlog.AuditEntryType
import com.vordain.guard.core.auditlog.AuditSeverity
import com.vordain.guard.core.auditlog.AuditTimeline
import com.vordain.guard.core.model.DeviceId
import java.nio.charset.StandardCharsets
import java.util.Base64

class ChildAuditStateStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): AuditTimeline {
        val entries = preferences.getString(KEY_ENTRIES, null)
            ?.lineSequence()
            ?.mapNotNull(::decodeEntry)
            ?.toList()
            ?: emptyList()
        return AuditTimeline(entries)
    }

    fun save(timeline: AuditTimeline) {
        preferences.edit()
            .putString(KEY_ENTRIES, timeline.entries.joinToString(separator = "\n", transform = ::encodeEntry))
            .apply()
    }

    private fun encodeEntry(entry: AuditEntry): String {
        return listOf(
            encode(entry.id),
            entry.type.name,
            entry.severity.name,
            entry.occurredAtMillis.toString(),
            encode(entry.title),
            encode(entry.detail),
            encode(entry.deviceId?.value.orEmpty()),
        ).joinToString(separator = "|")
    }

    private fun decodeEntry(line: String): AuditEntry? {
        val parts = line.split('|')
        if (parts.size != FIELD_COUNT) {
            return null
        }
        return runCatching {
            val deviceValue = decode(parts[6]).takeIf(String::isNotBlank)
            AuditEntry(
                id = decode(parts[0]),
                type = AuditEntryType.valueOf(parts[1]),
                severity = AuditSeverity.valueOf(parts[2]),
                occurredAtMillis = parts[3].toLong(),
                title = decode(parts[4]),
                detail = decode(parts[5]),
                deviceId = deviceValue?.let(::DeviceId),
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
        private const val PREFERENCES_NAME = "vordain_child_audit_state"
        private const val KEY_ENTRIES = "entries"
        private const val FIELD_COUNT = 7
    }
}
