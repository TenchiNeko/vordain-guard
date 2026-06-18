package com.vordain.guard.data.review

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.DomainName
import kotlin.test.Test
import kotlin.test.assertEquals

class ParentReviewModelsTest {
    @Test
    fun requestStoresMinimizedSubjectAndReason() {
        val subject = subject("school.example.edu", ReviewSubjectKind.SCHOOL_SITE)
        val request = ParentReviewRequest(
            requestId = "request-1",
            parentDeviceId = DeviceId("parent-device"),
            childDeviceId = DeviceId("child-device"),
            subject = subject,
            reason = ReviewRequestReason.SCHOOL_ACCESS,
            parentNote = "The site needed for class is not opening.",
            createdAtMillis = 1_000L,
        )

        assertEquals("request-1", request.requestId)
        assertEquals(subject, request.subject)
        assertEquals(ReviewRequestReason.SCHOOL_ACCESS, request.reason)
        assertEquals(1_000L, request.createdAtMillis)
    }

    @Test
    fun recommendationStoresRiskActionAndSummary() {
        val recommendation = ParentReviewRecommendation(
            requestId = "request-1",
            subject = subject("school.example.edu", ReviewSubjectKind.SCHOOL_SITE),
            riskLevel = ReviewRiskLevel.LOW,
            action = ReviewRecommendationAction.ALLOW_EXACT_DOMAIN,
            summary = "Looks appropriate for school access.",
            suggestedRule = null,
            reviewedAtMillis = 2_000L,
        )

        assertEquals(ReviewRiskLevel.LOW, recommendation.riskLevel)
        assertEquals(ReviewRecommendationAction.ALLOW_EXACT_DOMAIN, recommendation.action)
        assertEquals("Looks appropriate for school access.", recommendation.summary)
    }

    @Test
    fun suggestedRuleCanRepresentExactDomainAllow() {
        val rule = SuggestedAllowRule(
            domain = DomainName.from("school.example.edu"),
            appPackageName = null,
            scope = SuggestedAllowScope.EXACT_DOMAIN,
            expiresAtMillis = null,
        )

        assertEquals(SuggestedAllowScope.EXACT_DOMAIN, rule.scope)
        assertEquals(DomainName.from("school.example.edu"), rule.domain)
    }

    @Test
    fun suggestedRuleCanRepresentDomainAndSubdomainsAllow() {
        val rule = SuggestedAllowRule(
            domain = DomainName.from("example.edu"),
            appPackageName = null,
            scope = SuggestedAllowScope.DOMAIN_AND_SUBDOMAINS,
            expiresAtMillis = null,
        )

        assertEquals(SuggestedAllowScope.DOMAIN_AND_SUBDOMAINS, rule.scope)
        assertEquals(DomainName.from("example.edu"), rule.domain)
    }

    @Test
    fun appOnlySuggestedAllowIncludesPackageName() {
        val rule = SuggestedAllowRule(
            domain = DomainName.from("media.example"),
            appPackageName = AppPackageName("com.streaming.app"),
            scope = SuggestedAllowScope.APP_ONLY_DOMAIN,
            expiresAtMillis = null,
        )

        assertEquals(AppPackageName("com.streaming.app"), rule.appPackageName)
        assertEquals(SuggestedAllowScope.APP_ONLY_DOMAIN, rule.scope)
    }

    private fun subject(domain: String, kind: ReviewSubjectKind): ReviewSubject {
        return ReviewSubject(
            domain = DomainName.from(domain),
            appPackageName = null,
            kind = kind,
        )
    }
}
