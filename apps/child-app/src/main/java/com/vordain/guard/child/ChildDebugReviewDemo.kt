package com.vordain.guard.child

import com.vordain.guard.data.review.ParentReviewInputSanitizer
import com.vordain.guard.data.review.ReviewRequestReason
import com.vordain.guard.data.review.ReviewSubjectKind

class ChildDebugReviewDemo(
    private val sanitizer: ParentReviewInputSanitizer = ParentReviewInputSanitizer(),
) {
    fun buildReview(
        parentInput: String,
        reason: ReviewRequestReason,
    ): ChildDebugReviewResult {
        return runCatching {
            val subject = sanitizer.sanitize(
                parentEnteredSubject = parentInput,
                kind = when (reason) {
                    ReviewRequestReason.SCHOOL_ACCESS -> ReviewSubjectKind.SCHOOL_SITE
                    ReviewRequestReason.APP_NOT_WORKING -> ReviewSubjectKind.APP_COMPATIBILITY
                    ReviewRequestReason.SITE_NOT_WORKING,
                    ReviewRequestReason.POSSIBLE_FALSE_BLOCK -> ReviewSubjectKind.WEBSITE
                    ReviewRequestReason.PARENT_UNSURE -> ReviewSubjectKind.UNKNOWN
                },
            )

            ChildDebugReviewResult(
                sanitizedDomain = subject.domain.value,
                reason = reason.name,
                note = "Parent-initiated demo. Only the minimized domain is included.",
                error = null,
            )
        }.getOrElse { throwable ->
            ChildDebugReviewResult(
                sanitizedDomain = null,
                reason = reason.name,
                note = "Parent-initiated demo. Input was rejected locally.",
                error = throwable.message ?: "Review subject could not be sanitized",
            )
        }
    }
}

data class ChildDebugReviewResult(
    val sanitizedDomain: String?,
    val reason: String,
    val note: String,
    val error: String?,
) {
    fun asDisplayText(): String {
        if (error != null) {
            return "Reason: $reason\nError: $error\n$note"
        }

        return listOf(
            "Sanitized domain: $sanitizedDomain",
            "Reason: $reason",
            note,
        ).joinToString(separator = "\n")
    }
}
