package com.vordain.guard.data.local

import com.vordain.guard.core.policy.Policy

interface LocalPolicyStore {
    fun readPolicy(): Policy?

    fun writePolicy(policy: Policy)
}
