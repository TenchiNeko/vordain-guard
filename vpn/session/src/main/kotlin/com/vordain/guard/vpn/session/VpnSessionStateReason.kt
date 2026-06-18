package com.vordain.guard.vpn.session

enum class VpnSessionStateReason {
    INITIAL,
    USER_PERMISSION_REQUIRED,
    USER_PERMISSION_GRANTED,
    START_REQUESTED,
    STARTED,
    STOP_REQUESTED,
    STOPPED_BY_APP,
    REVOKED_BY_SYSTEM,
    PLATFORM_ERROR,
}
