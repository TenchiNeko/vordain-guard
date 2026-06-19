package com.vordain.guard.features.bypassrisk

import com.vordain.guard.core.model.DeviceId
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BypassRiskEvaluatorTest {
    private val evaluator = BypassRiskEvaluator()

    @Test
    fun emptyRiskListSummarizesNotReviewed() {
        val summary = evaluator.summarize(emptyList())

        assertEquals(BypassRiskOverallStatus.NOT_REVIEWED, summary.overallStatus)
    }

    @Test
    fun allCriticalItemsConfirmedSafeSummarizesReadyForDnsLab() {
        val summary = evaluator.summarize(allRequiredSafe())

        assertEquals(BypassRiskOverallStatus.READY_FOR_DNS_LAB, summary.overallStatus)
    }

    @Test
    fun privateDnsNotCheckedGivesNeedsAttention() {
        val summary = evaluator.summarize(
            allRequiredSafe().replaceCategory(BypassRiskCategory.PRIVATE_DNS, BypassRiskStatus.NOT_CHECKED),
        )

        assertEquals(BypassRiskOverallStatus.NEEDS_ATTENTION, summary.overallStatus)
    }

    @Test
    fun alternateVpnRiskFoundGivesHighRisk() {
        val summary = evaluator.summarize(
            allRequiredSafe().replaceCategory(BypassRiskCategory.ALTERNATE_VPN_APP, BypassRiskStatus.RISK_FOUND),
        )

        assertEquals(BypassRiskOverallStatus.HIGH_RISK, summary.overallStatus)
    }

    @Test
    fun proxyAppRiskFoundGivesHighRisk() {
        val summary = evaluator.summarize(
            allRequiredSafe().replaceCategory(BypassRiskCategory.PROXY_APP, BypassRiskStatus.RISK_FOUND),
        )

        assertEquals(BypassRiskOverallStatus.HIGH_RISK, summary.overallStatus)
    }

    @Test
    fun dohRiskFoundGivesHighRisk() {
        val summary = evaluator.summarize(
            allRequiredSafe().replaceCategory(BypassRiskCategory.DNS_OVER_HTTPS, BypassRiskStatus.RISK_FOUND),
        )

        assertEquals(BypassRiskOverallStatus.HIGH_RISK, summary.overallStatus)
    }

    @Test
    fun directIpRiskAddsWarningButDoesNotFailCriticalSetup() {
        val summary = evaluator.summarize(
            allRequiredSafe() + item(BypassRiskCategory.DIRECT_IP_ACCESS, BypassRiskStatus.NEEDS_REVIEW, BypassRiskSeverity.MEDIUM),
        )

        assertEquals(BypassRiskOverallStatus.READY_FOR_DNS_LAB, summary.overallStatus)
        assertTrue(summary.warningText.contains("direct-IP"))
    }

    @Test
    fun summaryWarningSaysNotFullProtection() {
        val summary = evaluator.summarize(allRequiredSafe())

        assertTrue(summary.warningText.contains("not full protection"))
    }

    @Test
    fun readinessRequiresReviewedRisksAndHardening() {
        val result = DnsOnlyReadinessEvaluator().evaluate(
            DnsOnlyReadinessInput(
                vpnPermissionConfirmed = true,
                alwaysOnVpnConfirmed = true,
                blockWithoutVpnConfirmed = true,
                settingsLockConfirmed = true,
                pinCompromiseSuspected = false,
                activePolicyVersion = "debug-1",
                dnsOnlyLabAvailable = true,
                localLabOnlyMode = false,
                bypassRiskSummary = evaluator.summarize(allRequiredSafe()),
            ),
        )

        assertEquals(DnsOnlyReadinessStatus.READY_FOR_DNS_LAB, result.status)
    }

    @Test
    fun readinessNeedsReviewWhenPrivateDnsIsNotReviewed() {
        val result = DnsOnlyReadinessEvaluator().evaluate(
            readyInput(
                bypassRiskSummary = evaluator.summarize(
                    allRequiredSafe().replaceCategory(BypassRiskCategory.PRIVATE_DNS, BypassRiskStatus.NOT_CHECKED),
                ),
            ),
        )

        assertEquals(DnsOnlyReadinessStatus.NEEDS_REVIEW, result.status)
    }

    @Test
    fun readinessHighRiskWhenAlternateVpnRiskFound() {
        val result = DnsOnlyReadinessEvaluator().evaluate(
            readyInput(
                bypassRiskSummary = evaluator.summarize(
                    allRequiredSafe().replaceCategory(BypassRiskCategory.ALTERNATE_VPN_APP, BypassRiskStatus.RISK_FOUND),
                ),
            ),
        )

        assertEquals(DnsOnlyReadinessStatus.HIGH_RISK, result.status)
    }

    @Test
    fun readinessHighRiskWhenPinCompromiseSuspected() {
        val result = DnsOnlyReadinessEvaluator().evaluate(readyInput(pinCompromiseSuspected = true))

        assertEquals(DnsOnlyReadinessStatus.HIGH_RISK, result.status)
    }

    @Test
    fun readinessNeedsReviewWhenPolicyMissing() {
        val result = DnsOnlyReadinessEvaluator().evaluate(readyInput(activePolicyVersion = null))

        assertEquals(DnsOnlyReadinessStatus.NEEDS_REVIEW, result.status)
    }

    @Test
    fun readinessDoesNotUseProtectedLabel() {
        val result = DnsOnlyReadinessEvaluator().evaluate(readyInput())

        assertFalse(result.toString().contains("Protected"))
    }

    @Test
    fun codecRoundTripsReportAndRejectsCredentialFields() {
        val codec = DebugBypassRiskReportCodec()
        val report = DebugBypassRiskReport(
            childDeviceId = DeviceId("child-debug-device"),
            generatedAtMillis = 1_000L,
            summary = evaluator.summarize(allRequiredSafe()),
        )

        val decoded = codec.decode(codec.encode(report)) as DebugBypassRiskReportCodecResult.Decoded
        val rejected = codec.decode(
            listOf(
                DebugBypassRiskReportCodec.HEADER,
                "childDeviceId=child-debug-device",
                "generatedAtMillis=1000",
                "pin" + "Value=123456",
            ).joinToString("\n"),
        )

        assertEquals("child-debug-device", decoded.report.childDeviceId.value)
        assertTrue(rejected is DebugBypassRiskReportCodecResult.Rejected)
    }

    @Test
    fun sourceHasNoForbiddenImportsOrGodClassNames() {
        val sourceRoot = repositoryRoot().resolve("features/bypass-risk/src/main")
        val source = sourceRoot.walkTopDown().filter { it.isFile }.joinToString("\n") { it.readText() }

        assertFalse(source.contains("android" + "."))
        assertFalse(source.contains("backend"))
        assertFalse(source.contains("data.relay"))
        assertFalse(source.contains("data.outbox"))
        assertFalse(source.contains("child " + "activity"))
        assertFalse(source.contains("traffic " + "log"))
        assertFalse(Regex("class .*" + "Man" + "ager|object .*" + "Man" + "ager").containsMatchIn(source))
        assertFalse(source.contains("TO" + "DO"))
        assertFalse(source.contains("FIX" + "ME"))
    }

    private fun readyInput(
        pinCompromiseSuspected: Boolean = false,
        activePolicyVersion: String? = "debug-1",
        bypassRiskSummary: BypassRiskSummary = evaluator.summarize(allRequiredSafe()),
    ): DnsOnlyReadinessInput {
        return DnsOnlyReadinessInput(
            vpnPermissionConfirmed = true,
            alwaysOnVpnConfirmed = true,
            blockWithoutVpnConfirmed = true,
            settingsLockConfirmed = true,
            pinCompromiseSuspected = pinCompromiseSuspected,
            activePolicyVersion = activePolicyVersion,
            dnsOnlyLabAvailable = true,
            localLabOnlyMode = false,
            bypassRiskSummary = bypassRiskSummary,
        )
    }

    private fun allRequiredSafe(): List<BypassRiskItem> {
        return BypassRiskEvaluator.REQUIRED_DNS_LAB_REVIEW_CATEGORIES.map { category ->
            item(category, BypassRiskStatus.CONFIRMED_SAFE, if (category == BypassRiskCategory.ALTERNATE_VPN_APP) BypassRiskSeverity.HIGH else BypassRiskSeverity.MEDIUM)
        }
    }

    private fun List<BypassRiskItem>.replaceCategory(
        category: BypassRiskCategory,
        status: BypassRiskStatus,
    ): List<BypassRiskItem> {
        return map { item ->
            if (item.category == category) {
                item.copy(status = status)
            } else {
                item
            }
        }
    }

    private fun item(
        category: BypassRiskCategory,
        status: BypassRiskStatus,
        severity: BypassRiskSeverity,
    ): BypassRiskItem {
        return BypassRiskItem(
            category = category,
            status = status,
            severity = severity,
            evidenceLabel = "Parent review",
            note = "Manual DNS-only lab review.",
        )
    }

    private fun repositoryRoot(): File {
        var current = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) return current
            current = current.parentFile ?: error("Could not find repository root")
        }
    }
}
