package com.vordain.guard.data.review

data class ParentReviewRecommendation(
    val requestId: String,
    val subject: ReviewSubject,
    val riskLevel: ReviewRiskLevel,
    val action: ReviewRecommendationAction,
    val summary: String,
    val suggestedRule: SuggestedAllowRule?,
    val reviewedAtMillis: Long,
)
