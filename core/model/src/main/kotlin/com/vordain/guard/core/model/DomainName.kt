package com.vordain.guard.core.model

import java.net.IDN
import java.util.Locale

/**
 * Represents a normalized hostname/domain only, not a URL.
 */
@JvmInline
value class DomainName private constructor(val value: String) {
    companion object {
        private const val MAX_DOMAIN_LENGTH = 253
        private const val MAX_LABEL_LENGTH = 63

        fun from(rawValue: String): DomainName {
            val trimmed = rawValue.trim()
            require(trimmed.isNotBlank()) { "Domain must not be blank" }
            require(trimmed.none { it.isWhitespace() }) { "Domain must not contain whitespace" }
            require(!trimmed.contains("://")) { "Domain must not include a URL scheme" }
            require(!trimmed.contains('/')) { "Domain must not include a path" }
            require(!trimmed.contains('\\')) { "Domain must not include a path" }
            require(!trimmed.contains(':')) { "Domain must not include a port" }

            val withoutRootDot = if (trimmed.endsWith(".")) trimmed.dropLast(1) else trimmed
            require(withoutRootDot.isNotBlank()) { "Domain must not be blank" }

            val normalizedLabels = withoutRootDot.split('.').map { label ->
                require(label.isNotEmpty()) { "Domain must not contain empty labels" }

                val asciiLabel = IDN.toASCII(label, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
                require(asciiLabel.isNotEmpty()) { "Domain must not contain empty labels" }
                require(asciiLabel.length <= MAX_LABEL_LENGTH) { "Domain label must be $MAX_LABEL_LENGTH characters or fewer" }
                require(!asciiLabel.startsWith("-") && !asciiLabel.endsWith("-")) {
                    "Domain labels must not start or end with hyphen"
                }

                asciiLabel
            }

            val normalized = normalizedLabels.joinToString(".")
            require(normalized.length <= MAX_DOMAIN_LENGTH) {
                "Domain must be $MAX_DOMAIN_LENGTH characters or fewer"
            }

            return DomainName(normalized)
        }
    }
}
