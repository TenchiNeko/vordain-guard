package com.vordain.guard.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AppTrafficModeTest {
    @Test
    fun containsExpectedTrafficModes() {
        assertEquals(
            listOf(
                AppTrafficMode.STRICT,
                AppTrafficMode.COMPATIBILITY,
                AppTrafficMode.BLOCKED,
                AppTrafficMode.MONITOR,
            ),
            AppTrafficMode.entries,
        )
    }
}
