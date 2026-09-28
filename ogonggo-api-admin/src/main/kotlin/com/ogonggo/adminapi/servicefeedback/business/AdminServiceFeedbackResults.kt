package com.ogonggo.adminapi.servicefeedback.business

import com.ogonggo.core.servicefeedback.domain.ServiceFeedback
import com.ogonggo.core.servicefeedback.implement.dto.ServiceFeedbackPageDto
import java.time.LocalDateTime

data class AdminServiceFeedbackPageResult(
    val items: List<AdminServiceFeedbackSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(result: ServiceFeedbackPageDto): AdminServiceFeedbackPageResult = AdminServiceFeedbackPageResult(
            items = result.serviceFeedbacks.map(AdminServiceFeedbackSummary::from),
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

data class AdminServiceFeedbackSummary(
    val id: Long,
    val userId: Long?,
    val satisfaction: String?,
    val improvement: String?,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(serviceFeedback: ServiceFeedback): AdminServiceFeedbackSummary = AdminServiceFeedbackSummary(
            id = checkNotNull(serviceFeedback.id) { "개선 의견 식별자가 없습니다." },
            userId = serviceFeedback.userId,
            satisfaction = serviceFeedback.satisfaction,
            improvement = serviceFeedback.improvement,
            registeredAt = serviceFeedback.createdAt,
        )
    }
}
