package com.vordain.guard.core.policy

enum class PolicyDecisionReason {
    ALLOWLIST_MATCH,
    BLOCKLIST_MATCH,
    UNKNOWN_DOMAIN_BLOCKED,
    PROXY_CATEGORY_BLOCKED,
    LOCKDOWN_MODE,
    NO_MATCH,
    INVALID_INPUT,
}
