package com.vordain.guard.vpn.classifier

import com.vordain.guard.core.model.DomainCategory
import com.vordain.guard.core.model.DomainClassification
import com.vordain.guard.core.model.DomainName
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaticRuleListClassifierTest {
    @Test
    fun exactProxyAnonymizerRuleReturnsProxyAnonymizerSignal() {
        val classification = classifier("proxy.example").classify(DomainName.from("proxy.example"))

        assertProxyAnonymizer(classification)
    }

    @Test
    fun directSubdomainReturnsProxyAnonymizerSignal() {
        val classification = classifier("proxy.example").classify(DomainName.from("www.proxy.example"))

        assertProxyAnonymizer(classification)
    }

    @Test
    fun deepSubdomainReturnsProxyAnonymizerSignal() {
        val classification = classifier("proxy.example").classify(DomainName.from("deep.www.proxy.example"))

        assertProxyAnonymizer(classification)
    }

    @Test
    fun unrelatedSiblingDoesNotReturnProxyAnonymizerSignal() {
        val classification = classifier("proxy.example").classify(DomainName.from("other.example"))

        assertUnknown(classification)
    }

    @Test
    fun suffixTrapsDoNotMatchProxyAnonymizerRule() {
        val classifier = classifier("proxy.example")

        assertUnknown(classifier.classify(DomainName.from("badproxy.example")))
        assertUnknown(classifier.classify(DomainName.from("proxy.example.evil.net")))
        assertUnknown(classifier.classify(DomainName.from("evil-proxy.example")))
    }

    @Test
    fun emptyRuleListReturnsUnknownClassification() {
        val classification = StaticRuleListClassifier(emptySet()).classify(DomainName.from("proxy.example"))

        assertUnknown(classification)
    }

    @Test
    fun classifierAcceptsAlreadyNormalizedDomainNameValues() {
        val classification = classifier("example.com").classify(DomainName.from("example.com"))

        assertProxyAnonymizer(classification)
    }

    @Test
    fun classifierUsesDomainNameNormalizationForCandidateAndRules() {
        val classification = classifier("example.com").classify(DomainName.from("EXAMPLE.COM."))

        assertProxyAnonymizer(classification)
    }

    @Test
    fun classifierTestsDoNotImportAndroidPackages() {
        val androidImportPrefix = "import " + "android" + "."

        assertSourceTreeDoesNotContain(
            sourceRoot = repositoryRoot().resolve("vpn/classifier/src/test/kotlin"),
            forbiddenText = androidImportPrefix,
        )
    }

    @Test
    fun classifierSourceDependsOnCoreModelButNotCorePolicy() {
        val sourceRoot = repositoryRoot().resolve("vpn/classifier/src/main/kotlin")
        val corePolicyPackage = listOf("com", "vordain", "guard", "core", "policy").joinToString(".")

        assertSourceTreeContains(sourceRoot, "com.vordain.guard.core.model")
        assertSourceTreeDoesNotContain(sourceRoot, corePolicyPackage)
    }

    private fun classifier(vararg proxyAnonymizerRules: String): StaticRuleListClassifier {
        return StaticRuleListClassifier(proxyAnonymizerRules.map { DomainName.from(it) }.toSet())
    }

    private fun assertProxyAnonymizer(classification: DomainClassification) {
        assertTrue(classification.contains(DomainCategory.PROXY_ANONYMIZER))
        assertFalse(classification.contains(DomainCategory.UNKNOWN))
    }

    private fun assertUnknown(classification: DomainClassification) {
        assertTrue(classification.contains(DomainCategory.UNKNOWN))
        assertFalse(classification.contains(DomainCategory.PROXY_ANONYMIZER))
    }

    private fun assertSourceTreeContains(sourceRoot: File, requiredText: String) {
        val kotlinFiles = kotlinFilesUnder(sourceRoot)
        assertTrue(
            actual = kotlinFiles.any { file -> file.readText().contains(requiredText) },
            message = "Expected source under ${sourceRoot.path} to contain $requiredText",
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
