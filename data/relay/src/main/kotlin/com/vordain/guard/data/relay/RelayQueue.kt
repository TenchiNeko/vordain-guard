package com.vordain.guard.data.relay

interface RelayQueue {
    fun enqueue(message: RelayMessage)

    fun pending(limit: Int): List<RelayMessage>

    fun remove(messageId: String)
}
