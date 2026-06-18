package com.vordain.guard.vpn.engine

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.policy.DefaultPolicyEngine
import com.vordain.guard.core.policy.Policy
import com.vordain.guard.core.policy.PolicyDecisionReason
import com.vordain.guard.vpn.classifier.StaticRuleListClassifier
import com.vordain.guard.vpn.dns.DnsMessageParser
import com.vordain.guard.vpn.dns.DnsParseFailureReason
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultDnsTrafficEvaluatorTest {
    @Test
    fun validDnsQueryForAllowlistedDomainReturnsAllow() {
        val decision = evaluator().evaluateDnsMessage(
            message = dnsQuery(question("example.com")),
            policy = basePolicy(allowedDomains = setOf(DomainName.from("example.com"))),
        )

        val allow = assertAllow(decision)
        assertEquals(1, allow.questionDecisions.size)
        assertEquals("example.com", allow.questionDecisions.single().question.domain.value)
        assertEquals(TrafficAction.ALLOW, allow.questionDecisions.single().trafficDecision.action)
    }

    @Test
    fun validDnsQueryForBlocklistedDomainReturnsBlock() {
        val decision = evaluator().evaluateDnsMessage(
            message = dnsQuery(question("example.com")),
            policy = basePolicy(blockedDomains = setOf(DomainName.from("example.com"))),
        )

        val block = assertBlock(decision)
        assertEquals(PolicyDecisionReason.BLOCKLIST_MATCH, block.questionDecisions.single().trafficDecision.evaluation.reason)
    }

    @Test
    fun validDnsQueryForSubdomainOfBlocklistedDomainReturnsBlock() {
        val decision = evaluator().evaluateDnsMessage(
            message = dnsQuery(question("login.example.com")),
            policy = basePolicy(blockedDomains = setOf(DomainName.from("example.com"))),
        )

        val block = assertBlock(decision)
        assertEquals("login.example.com", block.questionDecisions.single().question.domain.value)
        assertEquals(PolicyDecisionReason.BLOCKLIST_MATCH, block.questionDecisions.single().trafficDecision.evaluation.reason)
    }

    @Test
    fun proxyAnonymizerDnsQueryBlocksWhenPolicyBlocksKnownProxyDomains() {
        val decision = evaluator(proxyRules = setOf(DomainName.from("proxy.example"))).evaluateDnsMessage(
            message = dnsQuery(question("login.proxy.example")),
            policy = basePolicy(blockKnownProxyDomains = true),
        )

        val block = assertBlock(decision)
        assertEquals(
            PolicyDecisionReason.PROXY_CATEGORY_BLOCKED,
            block.questionDecisions.single().trafficDecision.evaluation.reason,
        )
    }

    @Test
    fun multipleQuestionsAggregateToBlockIfAnyQuestionBlocks() {
        val decision = evaluator().evaluateDnsMessage(
            message = dnsQuery(question("school.example"), question("blocked.example")),
            policy = basePolicy(
                allowedDomains = setOf(DomainName.from("school.example")),
                blockedDomains = setOf(DomainName.from("blocked.example")),
            ),
        )

        val block = assertBlock(decision)
        assertEquals(2, block.questionDecisions.size)
        assertEquals(TrafficAction.ALLOW, block.questionDecisions[0].trafficDecision.action)
        assertEquals(TrafficAction.BLOCK, block.questionDecisions[1].trafficDecision.action)
    }

    @Test
    fun multipleQuestionsAggregateToAlertOnlyIfNoneBlockAndAtLeastOneAlerts() {
        val decision = evaluator().evaluateDnsMessage(
            message = dnsQuery(question("school.example"), question("unknown.example")),
            policy = basePolicy(
                mode = LockdownMode.MONITOR_ONLY,
                allowedDomains = setOf(DomainName.from("school.example")),
                blockedDomains = emptySet(),
                blockUnknownDomains = false,
            ),
        )

        val alertOnly = assertAlertOnly(decision)
        assertEquals(2, alertOnly.questionDecisions.size)
        assertEquals(TrafficAction.ALLOW, alertOnly.questionDecisions[0].trafficDecision.action)
        assertEquals(TrafficAction.ALERT_ONLY, alertOnly.questionDecisions[1].trafficDecision.action)
    }

    @Test
    fun multipleQuestionsAggregateToAllowIfAllAllow() {
        val decision = evaluator().evaluateDnsMessage(
            message = dnsQuery(question("school.example"), question("library.example")),
            policy = basePolicy(
                allowedDomains = setOf(DomainName.from("school.example"), DomainName.from("library.example")),
                blockedDomains = emptySet(),
            ),
        )

        val allow = assertAllow(decision)
        assertEquals(2, allow.questionDecisions.size)
        assertTrue(allow.questionDecisions.all { it.trafficDecision.action == TrafficAction.ALLOW })
    }

    @Test
    fun malformedDnsMessageReturnsParseFailure() {
        val decision = evaluator().evaluateDnsMessage(
            message = ByteArray(11),
            policy = basePolicy(),
        )

        assertTrue(decision is DnsTrafficDecision.ParseFailure, "Expected parse failure but was $decision")
        assertEquals(DnsParseFailureReason.MESSAGE_TOO_SHORT, decision.failure.reason)
    }

    @Test
    fun perQuestionDecisionsArePreserved() {
        val decision = evaluator().evaluateDnsMessage(
            message = dnsQuery(question("school.example"), question("blocked.example")),
            policy = basePolicy(
                allowedDomains = setOf(DomainName.from("school.example")),
                blockedDomains = setOf(DomainName.from("blocked.example")),
            ),
        )

        val block = assertBlock(decision)
        assertEquals("school.example", block.questionDecisions[0].question.domain.value)
        assertEquals(1, block.questionDecisions[0].question.qtype)
        assertEquals(1, block.questionDecisions[0].question.qclass)
        assertEquals(TrafficAction.ALLOW, block.questionDecisions[0].trafficDecision.action)
        assertEquals("blocked.example", block.questionDecisions[1].question.domain.value)
        assertEquals(TrafficAction.BLOCK, block.questionDecisions[1].trafficDecision.action)
    }

    @Test
    fun engineSourceDoesNotImportAndroidOrVpnServicePackages() {
        val sourceRoot = repositoryRoot().resolve("vpn/engine/src/main/kotlin")
        val androidPackage = "android" + "."
        val vpnServicePackage = listOf("com", "vordain", "guard", "vpn", "service").joinToString(".")

        assertSourceTreeDoesNotContain(sourceRoot, androidPackage)
        assertSourceTreeDoesNotContain(sourceRoot, vpnServicePackage)
    }

    @Test
    fun dnsSourceStillDoesNotImportPolicyOrEnginePackages() {
        val sourceRoot = repositoryRoot().resolve("vpn/dns/src/main/kotlin")
        val corePolicyPackage = listOf("com", "vordain", "guard", "core", "policy").joinToString(".")
        val vpnEnginePackage = listOf("com", "vordain", "guard", "vpn", "engine").joinToString(".")

        assertSourceTreeDoesNotContain(sourceRoot, corePolicyPackage)
        assertSourceTreeDoesNotContain(sourceRoot, vpnEnginePackage)
    }

    private fun evaluator(
        proxyRules: Set<DomainName> = emptySet(),
    ): DefaultDnsTrafficEvaluator {
        return DefaultDnsTrafficEvaluator(
            dnsMessageParser = DnsMessageParser(),
            domainTrafficEvaluator = DefaultDomainTrafficEvaluator(
                domainClassifier = StaticRuleListClassifier(proxyRules),
                policyEngine = DefaultPolicyEngine(),
            ),
        )
    }

    private fun basePolicy(
        mode: LockdownMode = LockdownMode.STANDARD,
        allowedDomains: Set<DomainName> = emptySet(),
        blockedDomains: Set<DomainName> = emptySet(),
        blockUnknownDomains: Boolean = false,
        blockKnownProxyDomains: Boolean = true,
    ): Policy {
        return Policy(
            id = PolicyId("policy-test"),
            mode = mode,
            allowedDomains = allowedDomains,
            blockedDomains = blockedDomains,
            allowedPackages = setOf(AppPackageName("com.school.app")),
            blockedPackages = setOf(AppPackageName("com.blocked.app")),
            blockUnknownDomains = blockUnknownDomains,
            blockKnownProxyDomains = blockKnownProxyDomains,
        )
    }

    private fun assertAllow(decision: DnsTrafficDecision): DnsTrafficDecision.Allow {
        assertTrue(decision is DnsTrafficDecision.Allow, "Expected DNS allow decision but was $decision")
        return decision
    }

    private fun assertBlock(decision: DnsTrafficDecision): DnsTrafficDecision.Block {
        assertTrue(decision is DnsTrafficDecision.Block, "Expected DNS block decision but was $decision")
        return decision
    }

    private fun assertAlertOnly(decision: DnsTrafficDecision): DnsTrafficDecision.AlertOnly {
        assertTrue(decision is DnsTrafficDecision.AlertOnly, "Expected DNS alert-only decision but was $decision")
        return decision
    }

    private fun dnsQuery(vararg questions: ByteArray): ByteArray {
        return dnsHeader(questionCount = questions.size) + questions.fold(ByteArray(0)) { acc, question -> acc + question }
    }

    private fun dnsHeader(questionCount: Int): ByteArray {
        return byteArrayOf(
            0x12, 0x34,
            0x01, 0x00,
            highByte(questionCount), lowByte(questionCount),
            0x00, 0x00,
            0x00, 0x00,
            0x00, 0x00,
        )
    }

    private fun question(domain: String, qtype: Int = 1, qclass: Int = 1): ByteArray {
        return qname(domain) + byteArrayOf(highByte(qtype), lowByte(qtype), highByte(qclass), lowByte(qclass))
    }

    private fun qname(domain: String): ByteArray {
        return domain.split('.').fold(ByteArray(0)) { acc, label ->
            acc + byteArrayOf(label.length.toByte()) + label.encodeToByteArray()
        } + byteArrayOf(0)
    }

    private fun highByte(value: Int): Byte {
        return ((value ushr 8) and 0xFF).toByte()
    }

    private fun lowByte(value: Int): Byte {
        return (value and 0xFF).toByte()
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
