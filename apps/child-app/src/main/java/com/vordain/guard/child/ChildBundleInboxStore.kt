package com.vordain.guard.child

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64

data class ChildBundleInboxEntry(
    val id: String,
    val importedAtMillis: Long,
    val status: String,
    val sourceDeviceId: String,
    val targetDeviceId: String,
    val summary: String,
    val payloadLabels: List<String>,
    val appliedPolicyVersion: String,
)

class ChildBundleInboxStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<ChildBundleInboxEntry> {
        return preferences.getString(KEY_ENTRIES, null)
            ?.lineSequence()
            ?.mapNotNull(::decodeEntry)
            ?.toList()
            ?: emptyList()
    }

    fun save(entries: List<ChildBundleInboxEntry>) {
        preferences.edit()
            .putString(KEY_ENTRIES, entries.take(MAX_ENTRIES).joinToString(separator = "\n", transform = ::encodeEntry))
            .apply()
    }

    private fun encodeEntry(entry: ChildBundleInboxEntry): String {
        return listOf(
            encode(entry.id),
            entry.importedAtMillis.toString(),
            encode(entry.status),
            encode(entry.sourceDeviceId),
            encode(entry.targetDeviceId),
            encode(entry.summary),
            encode(entry.payloadLabels.joinToString(separator = LABEL_SEPARATOR)),
            encode(entry.appliedPolicyVersion),
        ).joinToString(separator = "|")
    }

    private fun decodeEntry(line: String): ChildBundleInboxEntry? {
        val parts = line.split('|')
        if (parts.size != FIELD_COUNT) {
            return null
        }
        return runCatching {
            ChildBundleInboxEntry(
                id = decode(parts[0]),
                importedAtMillis = parts[1].toLong(),
                status = decode(parts[2]),
                sourceDeviceId = decode(parts[3]),
                targetDeviceId = decode(parts[4]),
                summary = decode(parts[5]),
                payloadLabels = decode(parts[6]).split(LABEL_SEPARATOR).filter(String::isNotBlank),
                appliedPolicyVersion = decode(parts[7]),
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
        private const val PREFERENCES_NAME = "vordain_child_bundle_inbox"
        private const val KEY_ENTRIES = "entries"
        private const val FIELD_COUNT = 8
        private const val LABEL_SEPARATOR = " ; "
        const val MAX_ENTRIES = 20
    }
}
