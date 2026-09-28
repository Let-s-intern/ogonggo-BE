package com.ogonggo.core.servicefeedback.implement.dto

import com.ogonggo.core.servicefeedback.domain.ServiceFeedback

data class ServiceFeedbackAppendDto(
    val userId: Long?,
    val satisfaction: String?,
    val improvement: String?,
)

data class ServiceFeedbackPageDto(
    val serviceFeedbacks: List<ServiceFeedback>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
