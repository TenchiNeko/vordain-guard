package com.vordain.guard.vpn.dns

enum class DnsParseFailureReason {
    MESSAGE_TOO_SHORT,
    TRUNCATED_QUESTION,
    TRUNCATED_QNAME,
    QNAME_COMPRESSION_UNSUPPORTED,
    LABEL_TOO_LONG,
    QNAME_NOT_TERMINATED,
    INVALID_DOMAIN,
}
