package com.vordain.guard.core.policysync

enum class PolicyUpdateVerificationResult {
    Valid,
    Expired,
    NotYetValid,
    InvalidSignature,
    WrongDevice,
    Malformed,
}
