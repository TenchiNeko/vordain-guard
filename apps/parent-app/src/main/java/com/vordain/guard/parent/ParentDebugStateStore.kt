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
            pairingSessionId = preferences.getString(KEY_PAIRING_SESSION_ID, null)
                ?: ParentDebugStateSnapshot.DEFAULT_PAIRING_SESSION_ID,
            parentDeviceId = preferences.getString(KEY_PARENT_DEVICE_ID, null)
                ?: ParentDebugStateSnapshot.DEFAULT_PARENT_DEVICE_ID,
            parentDisplayName = preferences.getString(KEY_PARENT_DISPLAY_NAME, null)
                ?: ParentDebugStateSnapshot.DEFAULT_PARENT_DISPLAY_NAME,
            parentFingerprint = preferences.getString(KEY_PARENT_FINGERPRINT, null)
                ?: ParentDebugStateSnapshot.DEFAULT_PARENT_FINGERPRINT,
            verificationCode = preferences.getString(KEY_VERIFICATION_CODE, null)
                ?: ParentDebugStateSnapshot.DEFAULT_VERIFICATION_CODE,
            latestPairingInvitePayload = preferences.getString(KEY_LATEST_PAIRING_INVITE_PAYLOAD, null),
            latestPairingAcceptancePayload = preferences.getString(KEY_LATEST_PAIRING_ACCEPTANCE_PAYLOAD, null),
            acceptedChildSummary = preferences.getString(KEY_ACCEPTED_CHILD_SUMMARY, null),
            latestHardeningSetupReportPayload = preferences.getString(KEY_LATEST_HARDENING_SETUP_REPORT_PAYLOAD, null),
            latestChildSecurityStatusReportPayload = preferences.getString(
                KEY_LATEST_CHILD_SECURITY_STATUS_REPORT_PAYLOAD,
                null,
            ),
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
            .putString(KEY_PAIRING_SESSION_ID, snapshot.pairingSessionId)
            .putString(KEY_PARENT_DEVICE_ID, snapshot.parentDeviceId)
            .putString(KEY_PARENT_DISPLAY_NAME, snapshot.parentDisplayName)
            .putString(KEY_PARENT_FINGERPRINT, snapshot.parentFingerprint)
            .putString(KEY_VERIFICATION_CODE, snapshot.verificationCode)
            .putString(KEY_LATEST_PAIRING_INVITE_PAYLOAD, snapshot.latestPairingInvitePayload)
            .putString(KEY_LATEST_PAIRING_ACCEPTANCE_PAYLOAD, snapshot.latestPairingAcceptancePayload)
            .putString(KEY_ACCEPTED_CHILD_SUMMARY, snapshot.acceptedChildSummary)
            .putString(KEY_LATEST_HARDENING_SETUP_REPORT_PAYLOAD, snapshot.latestHardeningSetupReportPayload)
            .putString(
                KEY_LATEST_CHILD_SECURITY_STATUS_REPORT_PAYLOAD,
                snapshot.latestChildSecurityStatusReportPayload,
            )
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
        const val KEY_PAIRING_SESSION_ID = "pairing_session_id"
        const val KEY_PARENT_DEVICE_ID = "parent_device_id"
        const val KEY_PARENT_DISPLAY_NAME = "parent_display_name"
        const val KEY_PARENT_FINGERPRINT = "parent_fingerprint"
        const val KEY_VERIFICATION_CODE = "verification_code"
        const val KEY_LATEST_PAIRING_INVITE_PAYLOAD = "latest_pairing_invite_payload"
        const val KEY_LATEST_PAIRING_ACCEPTANCE_PAYLOAD = "latest_pairing_acceptance_payload"
        const val KEY_ACCEPTED_CHILD_SUMMARY = "accepted_child_summary"
        const val KEY_LATEST_HARDENING_SETUP_REPORT_PAYLOAD = "latest_hardening_setup_report_payload"
        const val KEY_LATEST_CHILD_SECURITY_STATUS_REPORT_PAYLOAD = "latest_child_security_status_report_payload"

        val persistedKeys = setOf(
            KEY_TARGET_CHILD_DEVICE_ID,
            KEY_POLICY_VERSION,
            KEY_ALLOW_DOMAINS_TEXT,
            KEY_BLOCK_DOMAINS_TEXT,
            KEY_BLOCK_KNOWN_PROXY_DOMAINS,
            KEY_BLOCK_UNKNOWN_DOMAINS,
            KEY_LATEST_GENERATED_PAYLOAD,
            KEY_PAIRING_SESSION_ID,
            KEY_PARENT_DEVICE_ID,
            KEY_PARENT_DISPLAY_NAME,
            KEY_PARENT_FINGERPRINT,
            KEY_VERIFICATION_CODE,
            KEY_LATEST_PAIRING_INVITE_PAYLOAD,
            KEY_LATEST_PAIRING_ACCEPTANCE_PAYLOAD,
            KEY_ACCEPTED_CHILD_SUMMARY,
            KEY_LATEST_HARDENING_SETUP_REPORT_PAYLOAD,
            KEY_LATEST_CHILD_SECURITY_STATUS_REPORT_PAYLOAD,
        )
    }
}
