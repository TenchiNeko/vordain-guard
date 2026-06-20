package com.vordain.guard.parent

import android.app.Activity

data class ParentRemoteTestConfig(
    val baseUrl: String,
    val parentDeviceId: String,
    val resultTargetDeviceId: String = "server-debug-device",
)

data class ParentRemoteTestActionResult(
    val success: Boolean,
    val summary: String,
)

data class ParentRemoteTestHandlers(
    val relayHealthCheck: () -> ParentRemoteTestActionResult,
    val sendPolicyBundle: () -> ParentRemoteTestActionResult,
    val fetchChildStatusMessages: () -> ParentRemoteTestActionResult,
    val ackFetchedMessage: () -> ParentRemoteTestActionResult,
    val exportDebugSnapshot: () -> ParentRemoteTestActionResult,
)

class ParentRemoteTestController(
    activity: Activity,
    relayClient: ParentLocalDevRelayClient = ParentLocalDevRelayClient(),
) {
    fun start(
        config: ParentRemoteTestConfig,
        handlers: ParentRemoteTestHandlers,
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
