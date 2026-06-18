package com.vordain.guard.data.outbox

interface RelayMessageIdProvider {
    fun nextId(): String
}
