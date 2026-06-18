package com.vordain.guard.vpn.session

data class VpnSessionSnapshot(
    val state: VpnSessionState,
    val reason: VpnSessionStateReason,
    val updatedAtMillis: Long,
    val message: String?,
) {
    companion object {
        fun initial(createdAtMillis: Long = 0L): VpnSessionSnapshot {
            return VpnSessionSnapshot(
                state = VpnSessionState.NOT_PREPARED,
                reason = VpnSessionStateReason.INITIAL,
                updatedAtMillis = createdAtMillis,
                message = null,
            )
        }
    }
}
