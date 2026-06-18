package com.vordain.guard.core.entitlement

enum class EntitlementEvaluationReason {
    ACTIVE,
    EXPIRED,
    NOT_YET_VALID,
    INVALID_SIGNATURE_FORMAT,
    INVALID_DEVICE_LIMIT,
    MALFORMED_LEASE,
}
