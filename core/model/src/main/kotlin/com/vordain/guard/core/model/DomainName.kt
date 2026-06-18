package com.vordain.guard.core.model

@JvmInline
value class DomainName private constructor(val value: String) {
    companion object {
        fun from(rawValue: String): DomainName {
            return DomainName(rawValue.trim().trimEnd('.').lowercase())
        }
    }
}
