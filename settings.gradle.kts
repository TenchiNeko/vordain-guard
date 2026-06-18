pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "vordain-guard"

include(":apps:parent-app")
include(":apps:child-app")

include(":core:model")
include(":core:policy")
include(":core:policy-sync")
include(":core:crypto")
include(":core:events")
include(":core:status")
include(":core:entitlement")
include(":core:intelligence")
include(":core:common")

include(":vpn:service")
include(":vpn:lifecycle")
include(":vpn:engine")
include(":vpn:classifier")
include(":vpn:dns")

include(":data:local")
include(":data:relay")
include(":data:outbox")
include(":data:heartbeat")
include(":data:review")

include(":features:pairing")
include(":features:parent-dashboard")
include(":features:child-status")
include(":features:policy-editor")
include(":features:alerts")
include(":features:setup-checklist")

include(":testkit")
