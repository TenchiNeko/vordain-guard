package com.vordain.guard.vpn.dns

import com.vordain.guard.core.model.DomainName
import java.nio.charset.StandardCharsets

class DnsMessageParser {
    fun parse(message: ByteArray): DnsParseResult {
        if (message.size < DNS_HEADER_LENGTH) {
            return failure(DnsParseFailureReason.MESSAGE_TOO_SHORT, "DNS message must include a 12-byte header")
        }

        val questionCount = readUInt16(message, QDCOUNT_OFFSET)
        val questions = mutableListOf<DnsQuestion>()
        var offset = DNS_HEADER_LENGTH

        repeat(questionCount) {
            val qnameResult = readQName(message, offset)
            when (qnameResult) {
                is QNameReadResult.Failure -> return qnameResult.toParseFailure()
                is QNameReadResult.Success -> {
                    offset = qnameResult.nextOffset
                    if (offset + QUESTION_FOOTER_LENGTH > message.size) {
                        return failure(
                            DnsParseFailureReason.TRUNCATED_QUESTION,
                            "DNS question is missing qtype or qclass",
                        )
                    }

                    val qtype = readUInt16(message, offset)
                    val qclass = readUInt16(message, offset + 2)
                    offset += QUESTION_FOOTER_LENGTH

                    questions += DnsQuestion(
                        domain = qnameResult.domain,
                        qtype = qtype,
                        qclass = qclass,
                    )
                }
            }
        }

        return DnsParseResult.Success(questions)
    }

    private fun readQName(message: ByteArray, startOffset: Int): QNameReadResult {
        val labels = mutableListOf<String>()
        var offset = startOffset

        while (true) {
            if (offset >= message.size) {
                return qnameFailure(
                    DnsParseFailureReason.QNAME_NOT_TERMINATED,
                    "DNS QNAME does not terminate with a zero byte",
                )
            }

            val lengthByte = message[offset].toInt() and 0xFF
            offset += 1

            if ((lengthByte and COMPRESSION_POINTER_MASK) == COMPRESSION_POINTER_MASK) {
                return qnameFailure(
                    DnsParseFailureReason.QNAME_COMPRESSION_UNSUPPORTED,
                    "DNS QNAME compression pointers are not supported by this parser",
                )
            }

            if (lengthByte > MAX_LABEL_LENGTH) {
                return qnameFailure(
                    DnsParseFailureReason.LABEL_TOO_LONG,
                    "DNS QNAME label length exceeds 63 bytes",
                )
            }

            if (lengthByte == 0) {
                val rawDomain = labels.joinToString(".")
                val domain = try {
                    DomainName.from(rawDomain)
                } catch (exception: IllegalArgumentException) {
                    return qnameFailure(
                        DnsParseFailureReason.INVALID_DOMAIN,
                        exception.message ?: "DNS QNAME is not a valid domain",
                    )
                }

                return QNameReadResult.Success(domain = domain, nextOffset = offset)
            }

            if (offset + lengthByte > message.size) {
                return qnameFailure(
                    DnsParseFailureReason.TRUNCATED_QNAME,
                    "DNS QNAME label is truncated",
                )
            }

            labels += String(message, offset, lengthByte, StandardCharsets.US_ASCII)
            offset += lengthByte
        }
    }

    private fun readUInt16(message: ByteArray, offset: Int): Int {
        return ((message[offset].toInt() and 0xFF) shl 8) or (message[offset + 1].toInt() and 0xFF)
    }

    private fun failure(reason: DnsParseFailureReason, message: String): DnsParseResult.Failure {
        return DnsParseResult.Failure(reason = reason, message = message)
    }

    private fun qnameFailure(reason: DnsParseFailureReason, message: String): QNameReadResult.Failure {
        return QNameReadResult.Failure(reason = reason, message = message)
    }

    private fun QNameReadResult.Failure.toParseFailure(): DnsParseResult.Failure {
        return failure(reason = reason, message = message)
    }

    private sealed interface QNameReadResult {
        data class Success(
            val domain: DomainName,
            val nextOffset: Int,
        ) : QNameReadResult

        data class Failure(
            val reason: DnsParseFailureReason,
            val message: String,
        ) : QNameReadResult
    }

    private companion object {
        const val DNS_HEADER_LENGTH = 12
        const val QDCOUNT_OFFSET = 4
        const val QUESTION_FOOTER_LENGTH = 4
        const val MAX_LABEL_LENGTH = 63
        const val COMPRESSION_POINTER_MASK = 0xC0
    }
}
