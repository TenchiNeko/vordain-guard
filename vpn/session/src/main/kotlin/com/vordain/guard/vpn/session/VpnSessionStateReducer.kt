package com.vordain.guard.vpn.session

class VpnSessionStateReducer {
    fun reduce(
        previous: VpnSessionSnapshot,
        command: VpnSessionCommand,
        currentTimeMillis: Long,
    ): VpnSessionTransition {
        val next = when (command) {
            is VpnSessionCommand.Prepare -> prepare(previous, command, currentTimeMillis)
            VpnSessionCommand.Start -> start(previous, currentTimeMillis)
            VpnSessionCommand.MarkStarted -> markStarted(previous, currentTimeMillis)
            VpnSessionCommand.Stop -> stop(previous, currentTimeMillis)
            VpnSessionCommand.MarkStopped -> markStopped(previous, currentTimeMillis)
            VpnSessionCommand.MarkRevoked -> snapshot(
                state = VpnSessionState.REVOKED,
                reason = VpnSessionStateReason.REVOKED_BY_SYSTEM,
                updatedAtMillis = currentTimeMillis,
            )
            is VpnSessionCommand.MarkError -> snapshot(
                state = VpnSessionState.ERROR,
                reason = VpnSessionStateReason.PLATFORM_ERROR,
                updatedAtMillis = currentTimeMillis,
                message = command.message,
            )
        }

        return if (next == null) {
            VpnSessionTransition(
                previous = previous,
                command = command,
                next = previous,
                accepted = false,
            )
        } else {
            VpnSessionTransition(
                previous = previous,
                command = command,
                next = next,
                accepted = true,
            )
        }
    }

    private fun prepare(
        previous: VpnSessionSnapshot,
        command: VpnSessionCommand.Prepare,
        currentTimeMillis: Long,
    ): VpnSessionSnapshot? {
        if (previous.state !in setOf(VpnSessionState.NOT_PREPARED, VpnSessionState.PERMISSION_REQUIRED)) {
            return null
        }
        return if (command.permissionGranted) {
            snapshot(
                state = VpnSessionState.READY,
                reason = VpnSessionStateReason.USER_PERMISSION_GRANTED,
                updatedAtMillis = currentTimeMillis,
            )
        } else {
            snapshot(
                state = VpnSessionState.PERMISSION_REQUIRED,
                reason = VpnSessionStateReason.USER_PERMISSION_REQUIRED,
                updatedAtMillis = currentTimeMillis,
            )
        }
    }

    private fun start(
        previous: VpnSessionSnapshot,
        currentTimeMillis: Long,
    ): VpnSessionSnapshot? {
        if (previous.state !in setOf(VpnSessionState.READY, VpnSessionState.STOPPED)) {
            return null
        }
        return snapshot(
            state = VpnSessionState.STARTING,
            reason = VpnSessionStateReason.START_REQUESTED,
            updatedAtMillis = currentTimeMillis,
        )
    }

    private fun markStarted(
        previous: VpnSessionSnapshot,
        currentTimeMillis: Long,
    ): VpnSessionSnapshot? {
        if (previous.state != VpnSessionState.STARTING) {
            return null
        }
        return snapshot(
            state = VpnSessionState.RUNNING,
            reason = VpnSessionStateReason.STARTED,
            updatedAtMillis = currentTimeMillis,
        )
    }

    private fun stop(
        previous: VpnSessionSnapshot,
        currentTimeMillis: Long,
    ): VpnSessionSnapshot? {
        if (previous.state !in setOf(VpnSessionState.STARTING, VpnSessionState.RUNNING)) {
            return null
        }
        return snapshot(
            state = VpnSessionState.STOPPING,
            reason = VpnSessionStateReason.STOP_REQUESTED,
            updatedAtMillis = currentTimeMillis,
        )
    }

    private fun markStopped(
        previous: VpnSessionSnapshot,
        currentTimeMillis: Long,
    ): VpnSessionSnapshot? {
        if (previous.state !in setOf(VpnSessionState.STARTING, VpnSessionState.RUNNING, VpnSessionState.STOPPING)) {
            return null
        }
        return snapshot(
            state = VpnSessionState.STOPPED,
            reason = VpnSessionStateReason.STOPPED_BY_APP,
            updatedAtMillis = currentTimeMillis,
        )
    }

    private fun snapshot(
        state: VpnSessionState,
        reason: VpnSessionStateReason,
        updatedAtMillis: Long,
        message: String? = null,
    ): VpnSessionSnapshot {
        return VpnSessionSnapshot(
            state = state,
            reason = reason,
            updatedAtMillis = updatedAtMillis,
            message = message,
        )
    }
}
