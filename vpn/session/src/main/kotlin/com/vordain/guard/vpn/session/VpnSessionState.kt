package com.vordain.guard.vpn.session

enum class VpnSessionState {
    NOT_PREPARED,
    PERMISSION_REQUIRED,
    READY,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    REVOKED,
    ERROR,
}
