package com.vordain.guard.data.review

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName

class ParentReviewInputSanitizer {
    fun sanitize(
        parentEnteredSubject: String,
        appPackageName: AppPackageName? = null,
        kind: ReviewSubjectKind = ReviewSubjectKind.UNKNOWN,
    ): ReviewSubject {
        val hostCandidate = extractHostCandidate(parentEnteredSubject)
        return ReviewSubject(
            domain = DomainName.from(hostCandidate),
            appPackageName = appPackageName,
            kind = kind,
        )
    }

    private fun extractHostCandidate(parentEnteredSubject: String): String {
        val trimmed = parentEnteredSubject.trim()
        require(trimmed.isNotBlank()) { "Review subject must not be blank" }

        val withoutScheme = trimmed.substringAfter("://", trimmed)
        val withoutFragment = withoutScheme.substringBefore('#')
        val withoutQuery = withoutFragment.substringBefore('?')
        val authority = withoutQuery.substringBefore('/')
        val withoutCredentials = authority.substringAfterLast('@')
        val withoutPort = stripPort(withoutCredentials)

        require(withoutPort.isNotBlank()) { "Review subject must contain a domain" }
        return withoutPort
    }

    private fun stripPort(value: String): String {
        val lastColonIndex = value.lastIndexOf(':')
        if (lastColonIndex <= 0) {
            return value
        }

        val possiblePort = value.substring(lastColonIndex + 1)
        return if (possiblePort.all(Char::isDigit)) {
            value.substring(0, lastColonIndex)
        } else {
            value
        }
    }
}
