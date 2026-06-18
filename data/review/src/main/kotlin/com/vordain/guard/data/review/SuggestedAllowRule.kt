package com.vordain.guard.data.review

import com.vordain.guard.core.model.AppPackageName
import com.vordain.guard.core.model.DomainName

data class SuggestedAllowRule(
    val domain: DomainName,
    val appPackageName: AppPackageName?,
    val scope: SuggestedAllowScope,
    val expiresAtMillis: Long?,
)
