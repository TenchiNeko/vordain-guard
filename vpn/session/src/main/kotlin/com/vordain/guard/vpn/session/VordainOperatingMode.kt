package com.vordain.guard.vpn.session

enum class VordainOperatingMode {
    IDLE,
    ESTABLISH_ONLY_SHELL,
    DNS_ONLY_LAB,
    BASIC_DNS_GUARD,
    FULL_TUNNEL_LAB,
}

data class VordainOperatingModeLabel(
    val displayName: String,
    val warningText: String,
    val usesDefaultRoute: Boolean,
    val inspectsNonDnsTraffic: Boolean,
    val forwardsNonDnsTraffic: Boolean,
    val productionReady: Boolean,
)

object VordainOperatingModeLabels {
    fun describe(mode: VordainOperatingMode): VordainOperatingModeLabel {
        return when (mode) {
            VordainOperatingMode.IDLE -> VordainOperatingModeLabel(
                displayName = "Idle",
                warningText = "No VPN mode is running.",
                usesDefaultRoute = false,
                inspectsNonDnsTraffic = false,
                forwardsNonDnsTraffic = false,
                productionReady = false,
            )
            VordainOperatingMode.ESTABLISH_ONLY_SHELL -> VordainOperatingModeLabel(
                displayName = "VPN shell",
                warningText = "Establish-only shell for setup testing. Filtering not production-enabled yet.",
                usesDefaultRoute = false,
                inspectsNonDnsTraffic = false,
                forwardsNonDnsTraffic = false,
                productionReady = false,
            )
            VordainOperatingMode.DNS_ONLY_LAB -> VordainOperatingModeLabel(
                displayName = "DNS-only lab",
                warningText = "DNS-only lab filtering. Non-DNS traffic is not inspected.",
                usesDefaultRoute = false,
                inspectsNonDnsTraffic = false,
                forwardsNonDnsTraffic = false,
                productionReady = false,
            )
            VordainOperatingMode.BASIC_DNS_GUARD -> VordainOperatingModeLabel(
                displayName = "Basic DNS Guard",
                warningText = "DNS-only enforcement. Non-DNS traffic is not inspected. Not full protection.",
                usesDefaultRoute = false,
                inspectsNonDnsTraffic = false,
                forwardsNonDnsTraffic = false,
                productionReady = false,
            )
            VordainOperatingMode.FULL_TUNNEL_LAB -> VordainOperatingModeLabel(
                displayName = "Full-tunnel lab",
                warningText = "Full-tunnel lab may stop internet while packets are captured locally.",
                usesDefaultRoute = true,
                inspectsNonDnsTraffic = true,
                forwardsNonDnsTraffic = false,
                productionReady = false,
            )
        }
    }
}
