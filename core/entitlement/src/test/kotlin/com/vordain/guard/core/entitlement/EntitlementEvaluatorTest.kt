package com.vordain.guard.core.entitlement

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntitlementEvaluatorTest {
    @Test
    fun activeLeaseEvaluatesActive() {
        val evaluation = evaluator.evaluate(lease(), currentTimeMillis = 1_500L)

        assertTrue(evaluation.active)
        assertEquals(EntitlementTier.GUARD_PLUS, evaluation.tier)
        assertEquals(EntitlementEvaluationReason.ACTIVE, evaluation.reason)
    }

    @Test
    fun expiredLeaseEvaluatesInactive() {
        val evaluation = evaluator.evaluate(lease(), currentTimeMillis = 2_000L)

        assertFalse(evaluation.active)
        assertEquals(EntitlementEvaluationReason.EXPIRED, evaluation.reason)
        assertEquals(emptySet(), evaluation.enabledFeatures)
    }

    @Test
    fun notYetValidLeaseEvaluatesInactive() {
        val evaluation = evaluator.evaluate(lease(), currentTimeMillis = 999L)

        assertFalse(evaluation.active)
        assertEquals(EntitlementEvaluationReason.NOT_YET_VALID, evaluation.reason)
    }

    @Test
    fun exactExpiryBoundaryIsExpired() {
        val evaluation = evaluator.evaluate(lease(expiresAtMillis = 1_500L), currentTimeMillis = 1_500L)

        assertFalse(evaluation.active)
        assertEquals(EntitlementEvaluationReason.EXPIRED, evaluation.reason)
    }

    @Test
    fun featureEnabledReturnsTrueForActiveLeaseWithFeature() {
        assertTrue(
            evaluator.isFeatureEnabled(
                lease = lease(features = setOf(EntitlementFeature.HEARTBEAT_ALERTS)),
                feature = EntitlementFeature.HEARTBEAT_ALERTS,
                currentTimeMillis = 1_500L,
            ),
        )
    }

    @Test
    fun featureEnabledReturnsFalseForExpiredLease() {
        assertFalse(
            evaluator.isFeatureEnabled(
                lease = lease(features = setOf(EntitlementFeature.HEARTBEAT_ALERTS)),
                feature = EntitlementFeature.HEARTBEAT_ALERTS,
                currentTimeMillis = 2_000L,
            ),
        )
    }

    @Test
    fun featureEnabledReturnsFalseWhenFeatureAbsent() {
        assertFalse(
            evaluator.isFeatureEnabled(
                lease = lease(features = setOf(EntitlementFeature.LOCAL_VPN_FILTERING)),
                feature = EntitlementFeature.HEARTBEAT_ALERTS,
                currentTimeMillis = 1_500L,
            ),
        )
    }

    @Test
    fun sourceDoesNotImportForbiddenPackagesOrForbiddenTerms() {
        val sourceRoot = repositoryRoot().resolve("core/entitlement/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "features").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "core", "events").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("Security", "Event").joinToString(""))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("Domain", "Name").joinToString(""))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("App", "Package", "Name").joinToString(""))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("browsing", "history").joinToString(" "))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("traffic", "log").joinToString(" "))
        assertSourceTreeDoesNotContain(sourceRoot, "screen" + "shot")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("message", "content").joinToString(" "))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("child", "activity").joinToString(" "))
    }

    private fun lease(
        expiresAtMillis: Long = 2_000L,
        features: Set<EntitlementFeature> = EntitlementDefaults.featuresFor(EntitlementTier.GUARD_PLUS),
    ): EntitlementLease {
        return EntitlementLease(
            accountId = AccountId("account-1"),
            tier = EntitlementTier.GUARD_PLUS,
            features = features,
            maxChildDevices = 3,
            issuedAtMillis = 1_000L,
            expiresAtMillis = expiresAtMillis,
            signature = EntitlementSignature("signature-1"),
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
        val evaluator = EntitlementEvaluator()
    }
}
