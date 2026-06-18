package com.vordain.guard.data.review

interface ParentReviewClient {
    fun submit(request: ParentReviewRequest): ParentReviewSubmitResult

    fun fetchRecommendation(requestId: String): ParentReviewRecommendationResult
}
