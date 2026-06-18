package com.vordain.guard.data.review

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParentReviewInputSanitizerTest {
    @Test
    fun plainDomainAccepted() {
        val subject = sanitizer.sanitize("school.example.edu", kind = ReviewSubjectKind.SCHOOL_SITE)

        assertEquals(DomainName.from("school.example.edu"), subject.domain)
        assertEquals(ReviewSubjectKind.SCHOOL_SITE, subject.kind)
    }

    @Test
    fun uppercaseAddressLowercasesDomain() {
        val subject = sanitizer.sanitize("HTTPS://School.Example.EDU")

        assertEquals(DomainName.from("school.example.edu"), subject.domain)
    }

    @Test
    fun pathAndQueryAreReducedToHostOnly() {
        val subject = sanitizer.sanitize("https://school.example.edu/login?student=123")

        assertEquals(DomainName.from("school.example.edu"), subject.domain)
    }

    @Test
    fun fragmentIsRemoved() {
        val subject = sanitizer.sanitize("https://school.example.edu/help#section")

        assertEquals(DomainName.from("school.example.edu"), subject.domain)
    }

    @Test
    fun portIsRemoved() {
        val subject = sanitizer.sanitize("https://school.example.edu:443/help")

        assertEquals(DomainName.from("school.example.edu"), subject.domain)
    }

    @Test
    fun usernameAndPasswordAreRemoved() {
        val subject = sanitizer.sanitize("https://student:secret@school.example.edu/help")

        assertEquals(DomainName.from("school.example.edu"), subject.domain)
    }

    @Test
    fun blankInputIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            sanitizer.sanitize("   ")
        }
    }

    @Test
    fun malformedDomainIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            sanitizer.sanitize("school..example.edu")
        }
    }

    @Test
    fun reducedSubjectDoesNotPreservePathOrQuery() {
        val subject = sanitizer.sanitize(
            parentEnteredSubject = "https://school.example.edu/login?student=123",
            appPackageName = AppPackageName("com.school.app"),
            kind = ReviewSubjectKind.APP_COMPATIBILITY,
        )

        assertEquals("school.example.edu", subject.domain.value)
        assertEquals(AppPackageName("com.school.app"), subject.appPackageName)
        assertEquals(ReviewSubjectKind.APP_COMPATIBILITY, subject.kind)
    }

    private companion object {
        val sanitizer = ParentReviewInputSanitizer()
    }
}
