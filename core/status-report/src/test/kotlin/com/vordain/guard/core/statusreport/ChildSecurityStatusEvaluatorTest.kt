package com.vordain.guard.core.statusreport

import com.vordain.guard.core.model.DeviceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChildSecurityStatusEvaluatorTest {
    private val evaluator = ChildSecurityStatusEvaluator()
    private val codec = DebugChildSecurityReportCodec()

    @Test
    fun readyStatusRequiresCoreVpnSetupSignals() {
        val report = evaluator.evaluate(baseInput())

        assertEquals(ChildSecurityOverallStatus.READY_FOR_LAB_TEST, report.overallStatus)
        assertTrue(ChildSecuritySignal.VPN_PERMISSION_CONFIRMED in report.signals)
        assertTrue(ChildSecuritySignal.VPN_ALWAYS_ON_CONFIRMED in report.signals)
        assertTrue(ChildSecuritySignal.BLOCK_WITHOUT_VPN_CONFIRMED in report.signals)
    }

    @Test
    fun missingBlockWithoutVpnNeedsAttention() {
        val report = evaluator.evaluate(baseInput(blockWithoutVpnConfirmed = false))

        assertEquals(ChildSecurityOverallStatus.NEEDS_ATTENTION, report.overallStatus)
    }

    @Test
    fun vpnStoppedWinsOverReady() {
        val report = evaluator.evaluate(baseInput(vpnStopped = true))

        assertEquals(ChildSecurityOverallStatus.VPN_STOPPED, report.overallStatus)
        assertTrue(ChildSecuritySignal.VPN_STOPPED in report.signals)
    }

    @Test
    fun pinCompromiseSignalWinsOverReady() {
        val report = evaluator.evaluate(baseInput(pinCompromiseSuspected = true))

        assertEquals(ChildSecurityOverallStatus.PIN_COMPROMISE_SUSPECTED, report.overallStatus)
        assertTrue(ChildSecuritySignal.PIN_COMPROMISE_SUSPECTED in report.signals)
    }

    @Test
    fun heartbeatFreshAloneDoesNotMakeReady() {
        val report = evaluator.evaluate(blankInput(heartbeatFresh = true))

        assertEquals(ChildSecurityOverallStatus.NEEDS_ATTENTION, report.overallStatus)
        assertTrue(ChildSecuritySignal.HEARTBEAT_FRESH in report.signals)
    }

    @Test
    fun policyAppliedAloneDoesNotMakeReady() {
        val report = evaluator.evaluate(blankInput(policyApplied = true, policyVersion = "debug-4"))

        assertEquals(ChildSecurityOverallStatus.NEEDS_ATTENTION, report.overallStatus)
        assertTrue(ChildSecuritySignal.POLICY_APPLIED in report.signals)
    }

    @Test
    fun reportWarningIsConservative() {
        val report = evaluator.evaluate(baseInput())

        assertEquals("Traffic filtering is not production-enabled yet.", report.warningText)
        assertFalse(report.warningText.contains("Protected"))
    }

    @Test
    fun codecRoundTripsReport() {
        val report = evaluator.evaluate(
            baseInput(
                policyApplied = true,
                policyVersion = "debug-4",
                activeMode = ChildSecurityActiveMode.DNS_ONLY_LAB,
                dnsBlockedResponseCount = 2,
                dnsAllowedForwardedCount = 3,
                dnsAllowedForwardFailureCount = 1,
                activePolicySource = "Verified debug policy",
                activePolicyPreset = "Basic DNS Guard",
                encryptedDnsBlockingEnabled = true,
                proxyBlockingEnabled = true,
                policyAllowDomainCount = 2,
                policyBlockDomainCount = 3,
            ),
        )

        val decoded = codec.decode(codec.encode(report))

        assertIs<DebugChildSecurityReportCodecResult.Decoded>(decoded)
        assertEquals(report.childDeviceId, decoded.report.childDeviceId)
        assertEquals(report.overallStatus, decoded.report.overallStatus)
        assertEquals(report.policyVersion, decoded.report.policyVersion)
        assertEquals(report.signals, decoded.report.signals)
        assertEquals(ChildSecurityActiveMode.DNS_ONLY_LAB, decoded.report.activeMode)
        assertEquals(2, decoded.report.dnsBlockedResponseCount)
        assertEquals(3, decoded.report.dnsAllowedForwardedCount)
        assertEquals(1, decoded.report.dnsAllowedForwardFailureCount)
        assertEquals("Verified debug policy", decoded.report.activePolicySource)
        assertEquals("Basic DNS Guard", decoded.report.activePolicyPreset)
        assertTrue(decoded.report.encryptedDnsBlockingEnabled)
        assertTrue(decoded.report.proxyBlockingEnabled)
        assertEquals(2, decoded.report.policyAllowDomainCount)
        assertEquals(3, decoded.report.policyBlockDomainCount)
    }

    @Test
    fun statusReportCanEncodeDecodeDnsOnlyMode() {
        val report = evaluator.evaluate(
            baseInput(
                activeMode = ChildSecurityActiveMode.DNS_ONLY_LAB,
                dnsBlockedResponseCount = 4,
                dnsAllowedForwardedCount = 5,
                dnsAllowedForwardFailureCount = 6,
            ),
        )

        val decoded = codec.decode(codec.encode(report))

        assertIs<DebugChildSecurityReportCodecResult.Decoded>(decoded)
        assertEquals(ChildSecurityActiveMode.DNS_ONLY_LAB, decoded.report.activeMode)
        assertEquals(4, decoded.report.dnsBlockedResponseCount)
        assertEquals(5, decoded.report.dnsAllowedForwardedCount)
        assertEquals(6, decoded.report.dnsAllowedForwardFailureCount)
    }

    @Test
    fun codecRejectsWrongHeader() {
        val decoded = codec.decode("NOPE\nchildDeviceId=child-debug-device")

        assertIs<DebugChildSecurityReportCodecResult.Rejected>(decoded)
    }

    @Test
    fun codecRejectsMissingChildDeviceId() {
        val decoded = codec.decode(
            """
            VORDAIN_DEBUG_CHILD_SECURITY_REPORT_V1
            generatedAtMillis=123
            overallStatus=READY_FOR_LAB_TEST
            """.trimIndent(),
        )

        assertIs<DebugChildSecurityReportCodecResult.Rejected>(decoded)
    }

    @Test
    fun codecRejectsUnknownStatus() {
        val decoded = codec.decode(
            """
            VORDAIN_DEBUG_CHILD_SECURITY_REPORT_V1
            childDeviceId=child-debug-device
            generatedAtMillis=123
            overallStatus=READY
            """.trimIndent(),
        )

        assertIs<DebugChildSecurityReportCodecResult.Rejected>(decoded)
    }

    @Test
    fun codecRejectsUnknownSignal() {
        val decoded = codec.decode(
            """
            VORDAIN_DEBUG_CHILD_SECURITY_REPORT_V1
            childDeviceId=child-debug-device
            generatedAtMillis=123
            overallStatus=READY_FOR_LAB_TEST
            signals=VPN_PERMISSION_CONFIRMED,UNKNOWN_SIGNAL
            """.trimIndent(),
        )

        assertIs<DebugChildSecurityReportCodecResult.Rejected>(decoded)
    }

    @Test
    fun codecRejectsForbiddenCredentialFields() {
        val decoded = codec.decode(
            """
            VORDAIN_DEBUG_CHILD_SECURITY_REPORT_V1
            childDeviceId=child-debug-device
            generatedAtMillis=123
            overallStatus=READY_FOR_LAB_TEST
            ${"pin"}${"Value"}=123456
            """.trimIndent(),
        )

        assertIs<DebugChildSecurityReportCodecResult.Rejected>(decoded)
    }

    private fun baseInput(
        vpnPermissionConfirmed: Boolean = true,
        alwaysOnVpnConfirmed: Boolean = true,
        blockWithoutVpnConfirmed: Boolean = true,
        settingsLockConfirmed: Boolean = true,
        policyApplied: Boolean = false,
        policyVersion: String? = null,
        vpnStopped: Boolean = false,
        pinCompromiseSuspected: Boolean = false,
        activeMode: ChildSecurityActiveMode = ChildSecurityActiveMode.NONE,
        dnsBlockedResponseCount: Long = 0,
        dnsAllowedForwardedCount: Long = 0,
        dnsAllowedForwardFailureCount: Long = 0,
        activePolicySource: String? = null,
        activePolicyPreset: String? = null,
        encryptedDnsBlockingEnabled: Boolean = true,
        proxyBlockingEnabled: Boolean = true,
        policyAllowDomainCount: Int = 0,
        policyBlockDomainCount: Int = 0,
    ): ChildSecurityStatusInput {
        return ChildSecurityStatusInput(
            childDeviceId = DeviceId("child-debug-device"),
            generatedAtMillis = 123L,
            vpnPermissionConfirmed = vpnPermissionConfirmed,
            alwaysOnVpnConfirmed = alwaysOnVpnConfirmed,
            blockWithoutVpnConfirmed = blockWithoutVpnConfirmed,
            settingsLockConfirmed = settingsLockConfirmed,
            developerOptionsDisabledConfirmed = true,
            adbDisabledConfirmed = true,
            noUnrestrictedProfilesConfirmed = true,
            policyVersion = policyVersion,
            policyApplied = policyApplied,
            vpnSessionLabel = "VPN shell active",
            vpnSessionRunning = true,
            vpnStopped = vpnStopped,
            heartbeatLabel = "Fresh",
            heartbeatFresh = true,
            setupSummaryLabel = "Ready for lab test",
            bypassRiskLabel = "No current signal",
            labCaptureActive = false,
            pinCompromiseSuspected = pinCompromiseSuspected,
            activeMode = activeMode,
            dnsBlockedResponseCount = dnsBlockedResponseCount,
            dnsAllowedForwardedCount = dnsAllowedForwardedCount,
            dnsAllowedForwardFailureCount = dnsAllowedForwardFailureCount,
            activePolicySource = activePolicySource,
            activePolicyPreset = activePolicyPreset,
            encryptedDnsBlockingEnabled = encryptedDnsBlockingEnabled,
            proxyBlockingEnabled = proxyBlockingEnabled,
            policyAllowDomainCount = policyAllowDomainCount,
            policyBlockDomainCount = policyBlockDomainCount,
        )
    }

    private fun blankInput(
        policyApplied: Boolean = false,
        policyVersion: String? = null,
        heartbeatFresh: Boolean = false,
    ): ChildSecurityStatusInput {
        return ChildSecurityStatusInput(
            childDeviceId = DeviceId("child-debug-device"),
            generatedAtMillis = 123L,
            vpnPermissionConfirmed = false,
            alwaysOnVpnConfirmed = false,
            blockWithoutVpnConfirmed = false,
            settingsLockConfirmed = false,
            developerOptionsDisabledConfirmed = false,
            adbDisabledConfirmed = false,
            noUnrestrictedProfilesConfirmed = false,
            policyVersion = policyVersion,
            policyApplied = policyApplied,
            vpnSessionLabel = "Not running",
            vpnSessionRunning = false,
            vpnStopped = false,
            heartbeatLabel = if (heartbeatFresh) "Fresh" else "Unknown",
            heartbeatFresh = heartbeatFresh,
            setupSummaryLabel = "Unknown",
            bypassRiskLabel = "No current signal",
            labCaptureActive = false,
            pinCompromiseSuspected = false,
        )
    }
}
