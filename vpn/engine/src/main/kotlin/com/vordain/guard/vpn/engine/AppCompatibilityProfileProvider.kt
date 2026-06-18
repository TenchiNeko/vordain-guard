package com.vordain.guard.vpn.engine

import com.vordain.guard.core.intelligence.AppCompatibilityProfile
import com.vordain.guard.core.model.AppPackageName

interface AppCompatibilityProfileProvider {
    fun profileFor(appPackageName: AppPackageName): AppCompatibilityProfile?
}
