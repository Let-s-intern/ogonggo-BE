package com.ogonggo.core.feedback.implement.dto

import com.ogonggo.core.feedback.domain.Feedback

data class FeedbackAppendDto(
    val userId: Long?,
    val satisfaction: String?,
    val improvement: String?,
)

data class FeedbackPageDto(
    val feedbacks: List<Feedback>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
