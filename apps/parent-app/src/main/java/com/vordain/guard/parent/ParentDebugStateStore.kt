package com.vordain.guard.parent

import android.content.Context

class ParentDebugStateStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): ParentDebugStateSnapshot {
        return ParentDebugStateSnapshot(
            targetChildDeviceId = preferences.getString(KEY_TARGET_CHILD_DEVICE_ID, null)
                ?: ParentDebugStateSnapshot.DEFAULT_TARGET_CHILD_DEVICE_ID,
            policyVersion = preferences.getString(KEY_POLICY_VERSION, null)
                ?: ParentDebugStateSnapshot.DEFAULT_POLICY_VERSION,
            allowDomainsText = preferences.getString(KEY_ALLOW_DOMAINS_TEXT, null)
                ?: ParentDebugStateSnapshot.DEFAULT_ALLOW_DOMAINS,
            blockDomainsText = preferences.getString(KEY_BLOCK_DOMAINS_TEXT, null)
                ?: ParentDebugStateSnapshot.DEFAULT_BLOCK_DOMAINS,
            blockKnownProxyDomains = preferences.getBoolean(KEY_BLOCK_KNOWN_PROXY_DOMAINS, true),
            blockUnknownDomains = preferences.getBoolean(KEY_BLOCK_UNKNOWN_DOMAINS, false),
            latestGeneratedPayload = preferences.getString(KEY_LATEST_GENERATED_PAYLOAD, null),
        )
    }

    fun save(snapshot: ParentDebugStateSnapshot) {
        preferences.edit()
            .putString(KEY_TARGET_CHILD_DEVICE_ID, snapshot.targetChildDeviceId)
            .putString(KEY_POLICY_VERSION, snapshot.policyVersion)
            .putString(KEY_ALLOW_DOMAINS_TEXT, snapshot.allowDomainsText)
            .putString(KEY_BLOCK_DOMAINS_TEXT, snapshot.blockDomainsText)
            .putBoolean(KEY_BLOCK_KNOWN_PROXY_DOMAINS, snapshot.blockKnownProxyDomains)
            .putBoolean(KEY_BLOCK_UNKNOWN_DOMAINS, snapshot.blockUnknownDomains)
            .putString(KEY_LATEST_GENERATED_PAYLOAD, snapshot.latestGeneratedPayload)
            .apply()
    }

    companion object {
        const val PREFERENCES_NAME = "vordain_parent_debug_state"
        const val KEY_TARGET_CHILD_DEVICE_ID = "target_child_device_id"
        const val KEY_POLICY_VERSION = "policy_version"
        const val KEY_ALLOW_DOMAINS_TEXT = "allow_domains_text"
        const val KEY_BLOCK_DOMAINS_TEXT = "block_domains_text"
        const val KEY_BLOCK_KNOWN_PROXY_DOMAINS = "block_known_proxy_domains"
        const val KEY_BLOCK_UNKNOWN_DOMAINS = "block_unknown_domains"
        const val KEY_LATEST_GENERATED_PAYLOAD = "latest_generated_payload"

        val persistedKeys = setOf(
            KEY_TARGET_CHILD_DEVICE_ID,
            KEY_POLICY_VERSION,
            KEY_ALLOW_DOMAINS_TEXT,
            KEY_BLOCK_DOMAINS_TEXT,
            KEY_BLOCK_KNOWN_PROXY_DOMAINS,
            KEY_BLOCK_UNKNOWN_DOMAINS,
            KEY_LATEST_GENERATED_PAYLOAD,
        )
    }
}
