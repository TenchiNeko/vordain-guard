package com.vordain.guard.child

import android.app.Activity
import com.vordain.guard.core.devrelay.DevRelayDebugCommand
import com.vordain.guard.core.devrelay.DevRelayDebugCommandResult
import com.vordain.guard.core.devrelay.DevRelayDebugCommandType
import com.vordain.guard.core.model.DeviceId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
    private val activity: Activity,
    private val relayClient: ChildLocalDevRelayClient = ChildLocalDevRelayClient(),
) {
    @Volatile
    private var active = false
    private var worker: Thread? = null
    private var latestStatus: String = "Remote test mode stopped."

    fun start(
        config: ChildRemoteTestConfig,
        handlers: ChildRemoteTestHandlers,
        onStatus: (String) -> Unit,
    ) {
        if (active) {
            onStatus(latestStatus)
            return
        }
        active = true
        latestStatus = "Remote test mode active for ${config.childDeviceId}. Local debug only."
        onStatus(latestStatus)
        worker = Thread {
            while (active) {
                pollOnce(config, handlers, onStatus)
                Thread.sleep(POLL_INTERVAL_MILLIS)
            }
        }.apply {
            name = "VordainChildRemoteTest"
            isDaemon = true
            start()
        }
    }

    fun stop() {
        active = false
        worker?.interrupt()
        worker = null
        latestStatus = "Remote test mode stopped."
    }

    fun isActive(): Boolean = active

    fun status(): String = latestStatus

    private fun pollOnce(
        config: ChildRemoteTestConfig,
        handlers: ChildRemoteTestHandlers,
        onStatus: (String) -> Unit,
    ) {
        try {
            val fetched = relayClient.fetchCommands(config.baseUrl, config.childDeviceId)
            val command = fetched.commands.firstOrNull()
            if (!fetched.success || command == null) {
                update("Remote test poll: ${fetched.summary}", onStatus)
                return
            }
            val result = execute(command, handlers)
            val relayResult = DevRelayDebugCommandResult(
                resultId = "child-result-${System.currentTimeMillis()}",
                commandId = command.commandId,
                type = command.type,
                sourceDeviceId = DeviceId(config.childDeviceId),
                targetDeviceId = DeviceId(config.resultTargetDeviceId),
                createdAtMillis = System.currentTimeMillis(),
                success = result.success,
                summary = result.summary.take(MAX_SUMMARY_CHARS),
            )
            val sent = relayClient.sendCommandResult(config.baseUrl, relayResult)
            update("Remote command ${command.type}: ${result.summary}\nResult post: ${sent.summary}", onStatus)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (error: Exception) {
            update("Remote test poll error: ${error::class.java.simpleName}: ${error.message.orEmpty()}", onStatus)
        }
    }

    private fun execute(
        command: DevRelayDebugCommand,
        handlers: ChildRemoteTestHandlers,
    ): ChildRemoteTestActionResult {
        return when (command.type) {
            DevRelayDebugCommandType.CHILD_RELAY_HEALTH_CHECK -> handlers.relayHealthCheck()
            DevRelayDebugCommandType.CHILD_FETCH_POLICY_BUNDLE -> runUiAction(handlers.fetchPolicyBundle)
            DevRelayDebugCommandType.CHILD_IMPORT_LATEST_POLICY_BUNDLE -> runUiAction(handlers.importLatestPolicyBundle)
            DevRelayDebugCommandType.CHILD_SEND_STATUS_BUNDLE -> runUiAction(handlers.sendChildStatusBundle)
            DevRelayDebugCommandType.CHILD_SEND_HEARTBEAT_STATUS_REPORT -> runUiAction(handlers.sendHeartbeatStatusReport)
            DevRelayDebugCommandType.CHILD_START_BASIC_DNS_GUARD -> runUiAction(handlers.startBasicDnsGuard)
            DevRelayDebugCommandType.CHILD_STOP_BASIC_DNS_GUARD -> runUiAction(handlers.stopBasicDnsGuard)
            else -> ChildRemoteTestActionResult(false, "Command ${command.type} is not allowlisted for child app.")
        }
    }

    private fun runUiAction(action: () -> ChildRemoteTestActionResult): ChildRemoteTestActionResult {
        var result = ChildRemoteTestActionResult(false, "UI action did not complete.")
        val latch = CountDownLatch(1)
        activity.runOnUiThread {
            try {
                result = action()
            } finally {
                latch.countDown()
            }
        }
        latch.await(UI_ACTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        return result
    }

    private fun update(text: String, onStatus: (String) -> Unit) {
        latestStatus = text
        activity.runOnUiThread {
            onStatus(text)
        }
    }

    companion object {
        private const val POLL_INTERVAL_MILLIS = 3_000L
        private const val UI_ACTION_TIMEOUT_MILLIS = 1_500L
        private const val MAX_SUMMARY_CHARS = 1_000
    }
}
