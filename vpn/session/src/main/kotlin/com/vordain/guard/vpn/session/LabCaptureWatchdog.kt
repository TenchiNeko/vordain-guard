package com.vordain.guard.vpn.session

data class LabCaptureWatchdogConfig(
    val enabled: Boolean = true,
    val maxSessionMillis: Long = DEFAULT_MAX_SESSION_MILLIS,
    val maxConsecutiveErrors: Int? = null,
) {
    init {
        require(maxSessionMillis > 0) { "maxSessionMillis must be positive" }
    }

    companion object {
        const val DEFAULT_MAX_SESSION_MILLIS: Long = 10L * 60L * 1_000L
    }
}

data class LabCaptureWatchdogState(
    val active: Boolean,
    val startedAtMillis: Long?,
    val expiresAtMillis: Long?,
    val reason: String,
) {
    companion object {
        fun inactive(reason: String = "Lab watchdog is inactive"): LabCaptureWatchdogState {
            return LabCaptureWatchdogState(
                active = false,
                startedAtMillis = null,
                expiresAtMillis = null,
                reason = reason,
            )
        }
    }
}

class LabCaptureWatchdog {
    fun start(
        config: LabCaptureWatchdogConfig,
        currentTimeMillis: Long,
        reason: String,
    ): LabCaptureWatchdogState {
        if (!config.enabled) {
            return LabCaptureWatchdogState.inactive("Lab watchdog disabled")
        }
        return LabCaptureWatchdogState(
            active = true,
            startedAtMillis = currentTimeMillis,
            expiresAtMillis = currentTimeMillis + config.maxSessionMillis,
            reason = reason,
        )
    }

    fun stop(reason: String): LabCaptureWatchdogState {
        return LabCaptureWatchdogState.inactive(reason)
    }

    fun isExpired(
        state: LabCaptureWatchdogState,
        currentTimeMillis: Long,
    ): Boolean {
        val expiresAt = state.expiresAtMillis ?: return false
        return state.active && currentTimeMillis >= expiresAt
    }
}
