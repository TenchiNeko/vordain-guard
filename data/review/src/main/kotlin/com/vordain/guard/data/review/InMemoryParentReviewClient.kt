package com.vordain.guard.data.review

class InMemoryParentReviewClient(
    private val submitResult: ParentReviewSubmitResult = ParentReviewSubmitResult.Accepted,
    private val fetchFailureResult: ParentReviewRecommendationResult? = null,
) : ParentReviewClient {
    private val submittedRequests = mutableListOf<ParentReviewRequest>()
    private val recommendationsByRequestId = linkedMapOf<String, ParentReviewRecommendation>()

    override fun submit(request: ParentReviewRequest): ParentReviewSubmitResult {
        if (submitResult == ParentReviewSubmitResult.Accepted) {
            submittedRequests += request
        }
        return submitResult
    }

    override fun fetchRecommendation(requestId: String): ParentReviewRecommendationResult {
        fetchFailureResult?.let { return it }

        val wasSubmitted = submittedRequests.any { request -> request.requestId == requestId }
        if (!wasSubmitted) {
            return ParentReviewRecommendationResult.NotFound
        }

        val recommendation = recommendationsByRequestId[requestId]
        return if (recommendation == null) {
            ParentReviewRecommendationResult.Pending
        } else {
            ParentReviewRecommendationResult.Available(recommendation)
        }
    }

    fun addRecommendation(recommendation: ParentReviewRecommendation) {
        recommendationsByRequestId[recommendation.requestId] = recommendation
    }

    fun submitted(): List<ParentReviewRequest> {
        return submittedRequests.toList()
    }
}
