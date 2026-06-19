package com.vordain.guard.parent

data class ParentDebugStateSnapshot(
    val targetChildDeviceId: String = DEFAULT_TARGET_CHILD_DEVICE_ID,
    val policyVersion: String = DEFAULT_POLICY_VERSION,
    val allowDomainsText: String = DEFAULT_ALLOW_DOMAINS,
    val blockDomainsText: String = DEFAULT_BLOCK_DOMAINS,
    val blockKnownProxyDomains: Boolean = true,
    val blockUnknownDomains: Boolean = false,
    val latestGeneratedPayload: String? = null,
) {
    companion object {
        const val DEFAULT_TARGET_CHILD_DEVICE_ID = "child-debug-device"
        const val DEFAULT_POLICY_VERSION = "debug-1"
        const val DEFAULT_ALLOW_DOMAINS = "school.edu"
        const val DEFAULT_BLOCK_DOMAINS = "proxy.example"
    }
}
