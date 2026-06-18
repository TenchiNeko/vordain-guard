package com.vordain.guard.core.events

interface EventIdProvider {
    fun nextId(): String
}
