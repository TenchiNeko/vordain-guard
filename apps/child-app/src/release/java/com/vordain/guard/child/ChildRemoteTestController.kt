package com.vordain.guard.child

import android.app.Activity

data class ChildRemoteTestConfig(
    val baseUrl: String,
    val childDeviceId: String,
    val resultTargetDeviceId: String = "server-debug-device",
)

data class ChildRemoteTestActionResult(
    val success: Boolean,
    val summary: String,
)

data class ChildRemoteTestHandlers(
    val relayHealthCheck: () -> ChildRemoteTestActionResult,
    val fetchPolicyBundle: () -> ChildRemoteTestActionResult,
    val importLatestPolicyBundle: () -> ChildRemoteTestActionResult,
    val sendChildStatusBundle: () -> ChildRemoteTestActionResult,
    val sendHeartbeatStatusReport: () -> ChildRemoteTestActionResult,
    val startBasicDnsGuard: () -> ChildRemoteTestActionResult,
    val stopBasicDnsGuard: () -> ChildRemoteTestActionResult,
)

class ChildRemoteTestController(
    activity: Activity,
    relayClient: ChildLocalDevRelayClient = ChildLocalDevRelayClient(),
) {
    fun start(
        config: ChildRemoteTestConfig,
        handlers: ChildRemoteTestHandlers,
        onStatus: (String) -> Unit,
    ) {
        onStatus("Remote test mode unavailable in release builds. No-op release implementation.")
    }

    fun stop() {
        // No-op release implementation.
    }

    fun isActive(): Boolean = false

    fun status(): String = "Remote test mode unavailable in release builds. No-op release implementation."
}
