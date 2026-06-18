package com.vordain.guard.data.review

import com.vordain.guard.core.model.DeviceId

data class ParentReviewRequest(
    val requestId: String,
    val parentDeviceId: DeviceId?,
    val childDeviceId: DeviceId?,
    val subject: ReviewSubject,
    val reason: ReviewRequestReason,
    val parentNote: String?,
    val createdAtMillis: Long,
)
