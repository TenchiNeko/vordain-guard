package com.vordain.guard.parent

import android.app.Activity
import com.vordain.guard.core.devrelay.DevRelayDebugCommand
import com.vordain.guard.core.devrelay.DevRelayDebugCommandResult
import com.vordain.guard.core.devrelay.DevRelayDebugCommandType
import com.vordain.guard.core.model.DeviceId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
    private val activity: Activity,
    private val relayClient: ParentLocalDevRelayClient = ParentLocalDevRelayClient(),
) {
    @Volatile
    private var active = false
    private var worker: Thread? = null
    private var latestStatus: String = "Remote test mode stopped."

    fun start(
        config: ParentRemoteTestConfig,
        handlers: ParentRemoteTestHandlers,
        onStatus: (String) -> Unit,
    ) {
        if (active) {
            onStatus(latestStatus)
            return
        }
        active = true
        latestStatus = "Remote test mode active for ${config.parentDeviceId}. Local debug only."
        onStatus(latestStatus)
        worker = Thread {
            while (active) {
                pollOnce(config, handlers, onStatus)
                Thread.sleep(POLL_INTERVAL_MILLIS)
            }
        }.apply {
            name = "VordainParentRemoteTest"
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
        config: ParentRemoteTestConfig,
        handlers: ParentRemoteTestHandlers,
        onStatus: (String) -> Unit,
    ) {
        try {
            val fetched = relayClient.fetchCommands(config.baseUrl, config.parentDeviceId)
            val command = fetched.commands.firstOrNull()
            if (!fetched.success || command == null) {
                update("Remote test poll: ${fetched.summary}", onStatus)
                return
            }
            val result = execute(command, handlers)
            val relayResult = DevRelayDebugCommandResult(
                resultId = "parent-result-${System.currentTimeMillis()}",
                commandId = command.commandId,
                type = command.type,
                sourceDeviceId = DeviceId(config.parentDeviceId),
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
        handlers: ParentRemoteTestHandlers,
    ): ParentRemoteTestActionResult {
        return when (command.type) {
            DevRelayDebugCommandType.PARENT_RELAY_HEALTH_CHECK -> handlers.relayHealthCheck()
            DevRelayDebugCommandType.PARENT_SEND_POLICY_BUNDLE -> runUiAction(handlers.sendPolicyBundle)
            DevRelayDebugCommandType.PARENT_FETCH_CHILD_STATUS_MESSAGES -> runUiAction(handlers.fetchChildStatusMessages)
            DevRelayDebugCommandType.PARENT_ACK_FETCHED_MESSAGE -> runUiAction(handlers.ackFetchedMessage)
            DevRelayDebugCommandType.PARENT_EXPORT_DEBUG_SNAPSHOT -> runUiAction(handlers.exportDebugSnapshot)
            else -> ParentRemoteTestActionResult(false, "Command ${command.type} is not allowlisted for parent app.")
        }
    }

    private fun runUiAction(action: () -> ParentRemoteTestActionResult): ParentRemoteTestActionResult {
        var result = ParentRemoteTestActionResult(false, "UI action did not complete.")
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
