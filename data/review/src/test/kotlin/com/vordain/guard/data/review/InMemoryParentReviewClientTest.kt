package com.vordain.guard.data.review

import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.DomainName
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemoryParentReviewClientTest {
    @Test
    fun clientStoresSubmittedRequest() {
        val client = InMemoryParentReviewClient()
        val request = request("request-1")

        val result = client.submit(request)

        assertEquals(ParentReviewSubmitResult.Accepted, result)
        assertEquals(listOf(request), client.submitted())
    }

    @Test
    fun requestOrderIsPreserved() {
        val client = InMemoryParentReviewClient()

        client.submit(request("request-1"))
        client.submit(request("request-2"))
        client.submit(request("request-3"))

        assertEquals(listOf("request-1", "request-2", "request-3"), client.submitted().map { it.requestId })
    }

    @Test
    fun pendingReturnedBeforeRecommendationExists() {
        val client = InMemoryParentReviewClient()
        client.submit(request("request-1"))

        assertEquals(ParentReviewRecommendationResult.Pending, client.fetchRecommendation("request-1"))
    }

    @Test
    fun availableReturnedAfterRecommendationIsAdded() {
        val client = InMemoryParentReviewClient()
        client.submit(request("request-1"))
        val recommendation = recommendation("request-1")

        client.addRecommendation(recommendation)

        assertEquals(
            ParentReviewRecommendationResult.Available(recommendation),
            client.fetchRecommendation("request-1"),
        )
    }

    @Test
    fun missingRequestReturnsNotFound() {
        val client = InMemoryParentReviewClient()

        assertEquals(ParentReviewRecommendationResult.NotFound, client.fetchRecommendation("missing"))
    }

    @Test
    fun retryableSubmitFailureIsConfigurable() {
        val client = InMemoryParentReviewClient(
            submitResult = ParentReviewSubmitResult.RetryableFailure("temporary unavailable"),
        )

        val result = client.submit(request("request-1"))

        assertEquals(ParentReviewSubmitResult.RetryableFailure("temporary unavailable"), result)
        assertTrue(client.submitted().isEmpty())
    }

    @Test
    fun permanentSubmitFailureIsConfigurable() {
        val client = InMemoryParentReviewClient(
            submitResult = ParentReviewSubmitResult.PermanentFailure("invalid request"),
        )

        val result = client.submit(request("request-1"))

        assertEquals(ParentReviewSubmitResult.PermanentFailure("invalid request"), result)
        assertTrue(client.submitted().isEmpty())
    }

    @Test
    fun retryableFetchFailureIsConfigurable() {
        val client = InMemoryParentReviewClient(
            fetchFailureResult = ParentReviewRecommendationResult.RetryableFailure("temporary unavailable"),
        )

        assertEquals(
            ParentReviewRecommendationResult.RetryableFailure("temporary unavailable"),
            client.fetchRecommendation("request-1"),
        )
    }

    @Test
    fun permanentFetchFailureIsConfigurable() {
        val client = InMemoryParentReviewClient(
            fetchFailureResult = ParentReviewRecommendationResult.PermanentFailure("invalid request"),
        )

        assertEquals(
            ParentReviewRecommendationResult.PermanentFailure("invalid request"),
            client.fetchRecommendation("request-1"),
        )
    }

    @Test
    fun reviewSourceDoesNotImportForbiddenPackagesOrPassiveDataTerms() {
        val sourceRoot = repositoryRoot().resolve("data/review/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "core", "events").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data", "relay").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "data", "outbox").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "vpn").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "apps").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "features").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("com", "vordain", "guard", "backend").joinToString("."))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("Security", "Event").joinToString(""))
        assertSourceTreeDoesNotContain(sourceRoot, "screen" + "shot")
        assertSourceTreeDoesNotContain(sourceRoot, listOf("browsing", "history").joinToString(" "))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("message", "content").joinToString(" "))
        assertSourceTreeDoesNotContain(sourceRoot, listOf("traffic", "log").joinToString(" "))
    }

    private fun request(id: String): ParentReviewRequest {
        return ParentReviewRequest(
            requestId = id,
            parentDeviceId = DeviceId("parent-device"),
            childDeviceId = DeviceId("child-device"),
            subject = ReviewSubject(
                domain = DomainName.from("school.example.edu"),
                appPackageName = null,
                kind = ReviewSubjectKind.SCHOOL_SITE,
            ),
            reason = ReviewRequestReason.SCHOOL_ACCESS,
            parentNote = null,
            createdAtMillis = 1_000L,
        )
    }

    private fun recommendation(requestId: String): ParentReviewRecommendation {
        val subject = ReviewSubject(
            domain = DomainName.from("school.example.edu"),
            appPackageName = null,
            kind = ReviewSubjectKind.SCHOOL_SITE,
        )
        return ParentReviewRecommendation(
            requestId = requestId,
            subject = subject,
            riskLevel = ReviewRiskLevel.LOW,
            action = ReviewRecommendationAction.ALLOW_EXACT_DOMAIN,
            summary = "Appropriate for school access.",
            suggestedRule = SuggestedAllowRule(
                domain = subject.domain,
                appPackageName = null,
                scope = SuggestedAllowScope.EXACT_DOMAIN,
                expiresAtMillis = null,
            ),
            reviewedAtMillis = 2_000L,
        )
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
