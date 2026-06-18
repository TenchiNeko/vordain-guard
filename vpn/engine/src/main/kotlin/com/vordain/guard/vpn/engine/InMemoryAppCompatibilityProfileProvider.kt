package com.vordain.guard.vpn.engine

import com.vordain.guard.core.intelligence.AppCompatibilityProfile
import com.vordain.guard.core.model.AppPackageName

class InMemoryAppCompatibilityProfileProvider(
    profiles: List<AppCompatibilityProfile>,
) : AppCompatibilityProfileProvider {
    private val profilesByPackageName = profiles.associateBy { profile -> profile.appPackageName }

    override fun profileFor(appPackageName: AppPackageName): AppCompatibilityProfile? {
        return profilesByPackageName[appPackageName]
    }
}
