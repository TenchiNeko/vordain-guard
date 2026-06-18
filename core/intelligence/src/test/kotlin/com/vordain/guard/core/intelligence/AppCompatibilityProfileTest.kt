package com.vordain.guard.core.intelligence

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppCompatibilityProfileTest {
    @Test
    fun appCompatibilityProfileStoresDomainSets() {
        val profile = profile()

        assertEquals(AppPackageName("com.school.app"), profile.appPackageName)
        assertEquals(setOf(DomainName.from("required.school.example")), profile.requiredDomains)
        assertEquals(setOf(DomainName.from("optional.school.example")), profile.optionalDomains)
        assertEquals(setOf(DomainName.from("blocked.school.example")), profile.blockedRegardlessDomains)
        assertEquals("profile-1", profile.profileVersion)
    }

    @Test
    fun invalidProfileConfidenceIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            profile(confidence = -1)
        }
    }

    private fun profile(confidence: Int = 90): AppCompatibilityProfile {
        return AppCompatibilityProfile(
            appPackageName = AppPackageName("com.school.app"),
            displayName = "School App",
            requiredDomains = setOf(DomainName.from("required.school.example")),
            optionalDomains = setOf(DomainName.from("optional.school.example")),
            blockedRegardlessDomains = setOf(DomainName.from("blocked.school.example")),
            profileVersion = "profile-1",
            confidence = confidence,
        )
    }
}
