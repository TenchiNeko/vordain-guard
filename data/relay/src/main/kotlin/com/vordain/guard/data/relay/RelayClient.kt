package com.vordain.guard.data.relay

interface RelayClient {
    /*
     * Payloads must be encrypted before this boundary. A relay implementation
     * must not accept readable child activity or decrypt parent-bound alerts.
     */
    fun send(message: RelayMessage): RelaySendResult
}
