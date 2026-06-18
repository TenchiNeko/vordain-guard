package com.vordain.guard.core.intelligence

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntelligenceBundleEvaluatorTest {
    @Test
    fun bundleExpiryBehaviorIsDeterministic() {
        val evaluator = IntelligenceBundleEvaluator(bundle(expiresAtMillis = 2_000L))

        assertFalse(evaluator.isExpired(1_999L))
        assertTrue(evaluator.isExpired(2_000L))
    }

    @Test
    fun bundleMalformedTimeBehaviorIsDeterministic() {
        val valid = IntelligenceBundleEvaluator(bundle(issuedAtMillis = 1_000L, expiresAtMillis = 2_000L))
        val malformed = IntelligenceBundleEvaluator(bundle(issuedAtMillis = 2_000L, expiresAtMillis = 2_000L))

        assertFalse(valid.isMalformed())
        assertTrue(malformed.isMalformed())
    }

    @Test
    fun findRecordMatchesExactDomain() {
        val evaluator = IntelligenceBundleEvaluator(bundle(domainRecords = listOf(record("example.com"))))

        assertNotNull(evaluator.findRecord(DomainName.from("example.com")))
    }

    @Test
    fun findRecordMatchesSubdomain() {
        val evaluator = IntelligenceBundleEvaluator(bundle(domainRecords = listOf(record("example.com"))))

        assertNotNull(evaluator.findRecord(DomainName.from("login.example.com")))
    }

    @Test
    fun findRecordAvoidsSuffixTrap() {
        val evaluator = IntelligenceBundleEvaluator(bundle(domainRecords = listOf(record("example.com"))))

        assertNull(evaluator.findRecord(DomainName.from("badexample.com")))
        assertNull(evaluator.findRecord(DomainName.from("example.com.evil.net")))
    }

    @Test
    fun findProfileMatchesPackageName() {
        val evaluator = IntelligenceBundleEvaluator(bundle(appProfiles = listOf(profile())))

        assertEquals(profile(), evaluator.findProfile(AppPackageName("com.school.app")))
        assertNull(evaluator.findProfile(AppPackageName("com.other.app")))
    }

    @Test
    fun classifyDomainReturnsCategories() {
        val evaluator = IntelligenceBundleEvaluator(
            bundle(
                domainRecords = listOf(
                    record(
                        domain = "cdn.school.example",
                        categories = setOf(DomainIntelligenceCategory.CDN, DomainIntelligenceCategory.APP_DEPENDENCY),
                    ),
                ),
            ),
        )

        assertEquals(
            setOf(DomainIntelligenceCategory.CDN, DomainIntelligenceCategory.APP_DEPENDENCY),
            evaluator.classifyDomain(DomainName.from("cdn.school.example")),
        )
    }

    @Test
    fun riskLevelForReturnsHighForProxyAnonymizerRecord() {
        val evaluator = IntelligenceBundleEvaluator(
            bundle(
                domainRecords = listOf(
                    record(
                        domain = "proxy.example",
                        categories = setOf(DomainIntelligenceCategory.PROXY_ANONYMIZER),
                        riskLevel = DomainRiskLevel.UNKNOWN,
                    ),
                ),
            ),
        )

        assertEquals(DomainRiskLevel.HIGH, evaluator.riskLevelFor(DomainName.from("proxy.example")))
    }

    @Test
    fun sourceDoesNotImportForbiddenPackagesOrGodClassNames() {
        val sourceRoot = repositoryRoot().resolve("core/intelligence/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "features").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "core", "events").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("Security", "Event").joinToString(""))
        assertSourceTreeDoesNotContain(sourceRoot, "TO" + "DO")
        assertSourceTreeDoesNotContain(sourceRoot, "FIX" + "ME")
        assertSourceTreeDoesNotContain(sourceRoot, "Manager")
    }

    private fun bundle(
        issuedAtMillis: Long = 1_000L,
        expiresAtMillis: Long = 2_000L,
        domainRecords: List<DomainIntelligenceRecord> = emptyList(),
        appProfiles: List<AppCompatibilityProfile> = emptyList(),
    ): IntelligenceBundle {
        return IntelligenceBundle(
            bundleVersion = "bundle-1",
            issuedAtMillis = issuedAtMillis,
            expiresAtMillis = expiresAtMillis,
            domainRecords = domainRecords,
            appProfiles = appProfiles,
            signature = IntelligenceBundleSignature("signature"),
        )
    }

    private fun record(
        domain: String,
        categories: Set<DomainIntelligenceCategory> = setOf(DomainIntelligenceCategory.PROXY_ANONYMIZER),
        riskLevel: DomainRiskLevel = DomainRiskLevel.UNKNOWN,
    ): DomainIntelligenceRecord {
        return DomainIntelligenceRecord(
            domain = DomainName.from(domain),
            categories = categories,
            riskLevel = riskLevel,
            confidence = 95,
            source = IntelligenceSource.HUMAN_REVIEW,
            reviewedAtMillis = 1_500L,
        )
    }

    private fun profile(): AppCompatibilityProfile {
        return AppCompatibilityProfile(
            appPackageName = AppPackageName("com.school.app"),
            displayName = "School App",
            requiredDomains = setOf(DomainName.from("required.school.example")),
            optionalDomains = setOf(DomainName.from("optional.school.example")),
            blockedRegardlessDomains = setOf(DomainName.from("blocked.school.example")),
            profileVersion = "profile-1",
            confidence = 90,
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
}
