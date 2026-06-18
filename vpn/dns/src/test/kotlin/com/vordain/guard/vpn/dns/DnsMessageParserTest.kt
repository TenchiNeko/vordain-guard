package com.vordain.guard.vpn.dns

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DnsMessageParserTest {
    private val parser = DnsMessageParser()

    @Test
    fun parsesValidAQueryForExampleCom() {
        val result = parser.parse(dnsQuery(question("example.com", qtype = 1, qclass = 1)))

        val success = assertSuccess(result)
        assertEquals(1, success.questions.size)
        assertEquals("example.com", success.questions.single().domain.value)
        assertEquals(1, success.questions.single().qtype)
        assertEquals(1, success.questions.single().qclass)
    }

    @Test
    fun parsesValidAaaaQueryForWwwExampleCom() {
        val result = parser.parse(dnsQuery(question("www.example.com", qtype = 28, qclass = 1)))

        val success = assertSuccess(result)
        assertEquals("www.example.com", success.questions.single().domain.value)
        assertEquals(28, success.questions.single().qtype)
        assertEquals(1, success.questions.single().qclass)
    }

    @Test
    fun parsesMultipleQuestions() {
        val result = parser.parse(
            dnsQuery(
                question("example.com", qtype = 1, qclass = 1),
                question("www.example.com", qtype = 28, qclass = 1),
            ),
        )

        val success = assertSuccess(result)
        assertEquals(2, success.questions.size)
        assertEquals("example.com", success.questions[0].domain.value)
        assertEquals(1, success.questions[0].qtype)
        assertEquals("www.example.com", success.questions[1].domain.value)
        assertEquals(28, success.questions[1].qtype)
    }

    @Test
    fun rejectsMessageShorterThanDnsHeader() {
        val result = parser.parse(ByteArray(11))

        assertFailure(DnsParseFailureReason.MESSAGE_TOO_SHORT, result)
    }

    @Test
    fun rejectsTruncatedQName() {
        val message = dnsHeader(questionCount = 1) + byteArrayOf(7) + "example".encodeToByteArray()

        val result = parser.parse(message)

        assertFailure(DnsParseFailureReason.QNAME_NOT_TERMINATED, result)
    }

    @Test
    fun rejectsQNameLabelLongerThanSixtyThreeBytes() {
        val message = dnsHeader(questionCount = 1) + byteArrayOf(64) + ByteArray(64) { 'a'.code.toByte() } +
            byteArrayOf(0, 0, 1, 0, 1)

        val result = parser.parse(message)

        assertFailure(DnsParseFailureReason.LABEL_TOO_LONG, result)
    }

    @Test
    fun rejectsQNameCompressionPointerAsUnsupported() {
        val message = dnsHeader(questionCount = 1) + byteArrayOf(0xC0.toByte(), 0x0C, 0, 1, 0, 1)

        val result = parser.parse(message)

        assertFailure(DnsParseFailureReason.QNAME_COMPRESSION_UNSUPPORTED, result)
    }

    @Test
    fun rejectsQNameWithoutTerminatingZeroByte() {
        val message = dnsHeader(questionCount = 1) + byteArrayOf(3) + "abc".encodeToByteArray()

        val result = parser.parse(message)

        assertFailure(DnsParseFailureReason.QNAME_NOT_TERMINATED, result)
    }

    @Test
    fun rejectsInvalidDomainNameFromParsedQName() {
        val result = parser.parse(dnsQuery(question("-example.com", qtype = 1, qclass = 1)))

        assertFailure(DnsParseFailureReason.INVALID_DOMAIN, result)
    }

    @Test
    fun dnsTestsDoNotImportAndroidPackages() {
        val androidImportPrefix = "import " + "android" + "."

        assertSourceTreeDoesNotContain(
            sourceRoot = repositoryRoot().resolve("vpn/dns/src/test/kotlin"),
            forbiddenText = androidImportPrefix,
        )
    }

    @Test
    fun dnsSourceDoesNotImportPolicyEngineOrVpnServicePackages() {
        val sourceRoot = repositoryRoot().resolve("vpn/dns/src/main/kotlin")
        val corePolicyPackage = listOf("com", "vordain", "guard", "core", "policy").joinToString(".")
        val vpnEnginePackage = listOf("com", "vordain", "guard", "vpn", "engine").joinToString(".")
        val vpnServicePackage = listOf("com", "vordain", "guard", "vpn", "service").joinToString(".")
        val androidPackage = "android" + "."

        assertSourceTreeDoesNotContain(sourceRoot, corePolicyPackage)
        assertSourceTreeDoesNotContain(sourceRoot, vpnEnginePackage)
        assertSourceTreeDoesNotContain(sourceRoot, vpnServicePackage)
        assertSourceTreeDoesNotContain(sourceRoot, androidPackage)
    }

    private fun assertSuccess(result: DnsParseResult): DnsParseResult.Success {
        assertTrue(result is DnsParseResult.Success, "Expected DNS parse success but was $result")
        return result
    }

    private fun assertFailure(expectedReason: DnsParseFailureReason, result: DnsParseResult) {
        assertTrue(result is DnsParseResult.Failure, "Expected DNS parse failure but was $result")
        assertEquals(expectedReason, result.reason)
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

    private fun question(domain: String, qtype: Int, qclass: Int): ByteArray {
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
