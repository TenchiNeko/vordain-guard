package com.vordain.guard.data.review

sealed interface ParentReviewRecommendationResult {
    data class Available(val recommendation: ParentReviewRecommendation) : ParentReviewRecommendationResult
    data object Pending : ParentReviewRecommendationResult
    data object NotFound : ParentReviewRecommendationResult
    data class RetryableFailure(val reason: String) : ParentReviewRecommendationResult
    data class PermanentFailure(val reason: String) : ParentReviewRecommendationResult
}
