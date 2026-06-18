package com.vordain.guard.data.review

enum class ReviewRecommendationAction {
    ALLOW_EXACT_DOMAIN,
    ALLOW_DOMAIN_AND_SUBDOMAINS,
    ALLOW_FOR_APP_ONLY,
    DENY,
    NEEDS_MORE_INFO,
}
