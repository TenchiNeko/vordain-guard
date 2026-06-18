package com.vordain.guard.core.events

interface EventClock {
    fun nowMillis(): Long
}
