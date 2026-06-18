package com.vordain.guard.data.outbox

interface OutboxClock {
    fun nowMillis(): Long
}
