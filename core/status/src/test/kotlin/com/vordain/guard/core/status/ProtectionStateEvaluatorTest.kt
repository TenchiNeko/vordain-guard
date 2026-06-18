package com.vordain.guard.core.status

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.model.ProtectionState
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtectionStateEvaluatorTest {
    @Test
    fun freshHealthyReportEvaluatesToProtectedState() {
        val evaluation = evaluate(report = healthyReport())

        assertEquals(ProtectionState.PROTECTED, evaluation.state)
        assertFalse(evaluation.shouldAlertParent)
        assertEquals(
            setOf(
                ProtectionStatusReason.HEARTBEAT_FRESH,
                ProtectionStatusReason.VPN_ACTIVE,
                ProtectionStatusReason.POLICY_LOADED,
            ),
            evaluation.reasons,
        )
    }

    @Test
    fun missingReportEvaluatesToUnknown() {
        val evaluation = evaluate(report = null)

        assertEquals(ProtectionState.UNKNOWN, evaluation.state)
        assertTrue(evaluation.shouldAlertParent)
        assertEquals(setOf(ProtectionStatusReason.HEARTBEAT_MISSING), evaluation.reasons)
    }

    @Test
    fun staleReportEvaluatesToUnknown() {
        val evaluation = evaluate(report = healthyReport(reportedAtMillis = 1_000L), currentTimeMillis = 10_001L)

        assertEquals(ProtectionState.UNKNOWN, evaluation.state)
        assertTrue(evaluation.shouldAlertParent)
        assertEquals(setOf(ProtectionStatusReason.HEARTBEAT_STALE), evaluation.reasons)
    }

    @Test
    fun freshReportWithStoppedVpnEvaluatesToStopped() {
        val evaluation = evaluate(report = healthyReport(vpnActive = false))

        assertEquals(ProtectionState.STOPPED, evaluation.state)
        assertTrue(evaluation.shouldAlertParent)
        assertTrue(ProtectionStatusReason.VPN_STOPPED in evaluation.reasons)
    }

    @Test
    fun freshReportWithLocalTamperEvaluatesToStopped() {
        val evaluation = evaluate(report = healthyReport(localTamperDetected = true))

        assertEquals(ProtectionState.STOPPED, evaluation.state)
        assertTrue(evaluation.shouldAlertParent)
        assertTrue(ProtectionStatusReason.LOCAL_TAMPER_DETECTED in evaluation.reasons)
    }

    @Test
    fun freshReportWithMissingPolicyEvaluatesToDegraded() {
        val evaluation = evaluate(report = healthyReport(policyLoaded = false, policyVersion = null))

        assertEquals(ProtectionState.DEGRADED, evaluation.state)
        assertTrue(evaluation.shouldAlertParent)
        assertTrue(ProtectionStatusReason.POLICY_MISSING in evaluation.reasons)
    }

    @Test
    fun freshReportWithAlwaysOnVpnDisabledEvaluatesToDegraded() {
        val evaluation = evaluate(report = healthyReport(alwaysOnVpnEnabled = false))

        assertEquals(ProtectionState.DEGRADED, evaluation.state)
        assertTrue(evaluation.shouldAlertParent)
        assertTrue(ProtectionStatusReason.ALWAYS_ON_VPN_DISABLED in evaluation.reasons)
    }

    @Test
    fun freshReportWithBlockWithoutVpnDisabledEvaluatesToDegraded() {
        val evaluation = evaluate(report = healthyReport(blockWithoutVpnEnabled = false))

        assertEquals(ProtectionState.DEGRADED, evaluation.state)
        assertTrue(evaluation.shouldAlertParent)
        assertTrue(ProtectionStatusReason.BLOCK_WITHOUT_VPN_DISABLED in evaluation.reasons)
    }

    @Test
    fun freshReportWithAppProtectionDisabledEvaluatesToDegraded() {
        val evaluation = evaluate(report = healthyReport(appProtectionEnabled = false))

        assertEquals(ProtectionState.DEGRADED, evaluation.state)
        assertTrue(evaluation.shouldAlertParent)
        assertTrue(ProtectionStatusReason.APP_PROTECTION_DISABLED in evaluation.reasons)
    }

    @Test
    fun degradedStateUsesParentVisibleWarningBehavior() {
        val evaluation = evaluate(report = healthyReport(alwaysOnVpnEnabled = false))

        assertEquals(ProtectionState.DEGRADED, evaluation.state)
        assertTrue(evaluation.shouldAlertParent)
        assertEquals("Protection is active but setup is incomplete or weakened.", evaluation.summary)
    }

    @Test
    fun staleStoppedReportIsUnknownNotStopped() {
        val evaluation = evaluate(
            report = healthyReport(reportedAtMillis = 1_000L, vpnActive = false),
            currentTimeMillis = 10_001L,
        )

        assertEquals(ProtectionState.UNKNOWN, evaluation.state)
        assertTrue(evaluation.shouldAlertParent)
        assertFalse(ProtectionStatusReason.VPN_STOPPED in evaluation.reasons)
    }

    @Test
    fun sourceDoesNotImportForbiddenPackages() {
        val sourceRoot = repositoryRoot().resolve("core/status/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "features").joinToString("."))
    }

    private fun evaluate(
        report: ProtectionReport?,
        currentTimeMillis: Long = 10_000L,
        heartbeatTimeoutMillis: Long = 5_000L,
    ): ProtectionEvaluation {
        return ProtectionStateEvaluator().evaluate(
            deviceId = deviceId,
            latestReport = report,
            currentTimeMillis = currentTimeMillis,
            heartbeatTimeoutMillis = heartbeatTimeoutMillis,
        )
    }

    private fun healthyReport(
        reportedAtMillis: Long = 9_000L,
        vpnActive: Boolean = true,
        policyLoaded: Boolean = true,
        policyVersion: PolicyId? = PolicyId("policy-1"),
        alwaysOnVpnEnabled: Boolean = true,
        blockWithoutVpnEnabled: Boolean = true,
        appProtectionEnabled: Boolean = true,
        localTamperDetected: Boolean = false,
    ): ProtectionReport {
        return ProtectionReport(
            deviceId = deviceId,
            reportedAtMillis = reportedAtMillis,
            vpnActive = vpnActive,
            policyLoaded = policyLoaded,
            policyVersion = policyVersion,
            alwaysOnVpnEnabled = alwaysOnVpnEnabled,
            blockWithoutVpnEnabled = blockWithoutVpnEnabled,
            appProtectionEnabled = appProtectionEnabled,
            localTamperDetected = localTamperDetected,
        )
    }

    private fun assertSourceTreeDoesNotContain(sourceRoot: File, forbiddenText: String) {
        val filesWithForbiddenText = kotlinFilesUnder(sourceRoot).filter { file ->
            file.readText().contains(forbiddenText)
        }

        assertTrue(
            actual = filesWithForbiddenText.isEmpty(),
            message = "Forbidden text $forbiddenText found in ${filesWithForbiddenText.map { it.path }}",
        )
    }

    private fun kotlinFilesUnder(sourceRoot: File): List<File> {
        val kotlinFiles = sourceRoot.walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .toList()

        assertTrue(kotlinFiles.isNotEmpty(), "Expected Kotlin files under ${sourceRoot.path}")
        return kotlinFiles
    }

    private fun repositoryRoot(): File {
        var current = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) {
                return current
            }
            current = current.parentFile ?: error("Could not find repository root from ${System.getProperty("user.dir")}")
        }
    }

    private companion object {
        val deviceId = DeviceId("child-device")
    }
}
