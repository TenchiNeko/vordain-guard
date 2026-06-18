package com.vordain.guard.vpn.dns

sealed interface DnsParseResult {
    data class Success(val questions: List<DnsQuestion>) : DnsParseResult

    data class Failure(val reason: DnsParseFailureReason, val message: String) : DnsParseResult
}
