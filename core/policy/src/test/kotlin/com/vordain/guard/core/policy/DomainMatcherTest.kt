package com.vordain.guard.core.policy

import com.vordain.guard.core.model.DomainName
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DomainMatcherTest {
    @Test
    fun exactDomainMatchesRule() {
        assertTrue(DomainMatcher.matches(DomainName.from("example.com"), DomainName.from("example.com")))
    }

    @Test
    fun directSubdomainMatchesParentRule() {
        assertTrue(DomainMatcher.matches(DomainName.from("www.example.com"), DomainName.from("example.com")))
    }

    @Test
    fun deepSubdomainMatchesParentRule() {
        assertTrue(DomainMatcher.matches(DomainName.from("deep.www.example.com"), DomainName.from("example.com")))
    }

    @Test
    fun siblingDomainDoesNotMatchRule() {
        assertFalse(DomainMatcher.matches(DomainName.from("other.com"), DomainName.from("example.com")))
    }

    @Test
    fun suffixTrapDoesNotMatchRule() {
        assertFalse(DomainMatcher.matches(DomainName.from("badexample.com"), DomainName.from("example.com")))
        assertFalse(DomainMatcher.matches(DomainName.from("example.com.evil.net"), DomainName.from("example.com")))
        assertFalse(DomainMatcher.matches(DomainName.from("evil-example.com"), DomainName.from("example.com")))
    }

    @Test
    fun schoolDomainAndLoginSubdomainMatchSchoolRule() {
        val rule = DomainName.from("school.edu")

        assertTrue(DomainMatcher.matches(DomainName.from("school.edu"), rule))
        assertTrue(DomainMatcher.matches(DomainName.from("login.school.edu"), rule))
    }
}
