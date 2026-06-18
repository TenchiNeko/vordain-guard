package com.vordain.guard.data.review

sealed interface ParentReviewSubmitResult {
    data object Accepted : ParentReviewSubmitResult
    data class RetryableFailure(val reason: String) : ParentReviewSubmitResult
    data class PermanentFailure(val reason: String) : ParentReviewSubmitResult
}
