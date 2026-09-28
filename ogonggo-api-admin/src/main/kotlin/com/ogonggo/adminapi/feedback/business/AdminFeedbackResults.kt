package com.ogonggo.adminapi.feedback.business

import com.ogonggo.core.feedback.domain.Feedback
import com.ogonggo.core.feedback.implement.dto.FeedbackPageDto
import java.time.LocalDateTime

data class AdminFeedbackPageResult(
    val items: List<AdminFeedbackSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(result: FeedbackPageDto): AdminFeedbackPageResult = AdminFeedbackPageResult(
            items = result.feedbacks.map(AdminFeedbackSummary::from),
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

data class AdminFeedbackSummary(
    val id: Long,
    val userId: Long?,
    val satisfaction: String?,
    val improvement: String?,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(feedback: Feedback): AdminFeedbackSummary = AdminFeedbackSummary(
            id = checkNotNull(feedback.id) { "개선 의견 식별자가 없습니다." },
            userId = feedback.userId,
            satisfaction = feedback.satisfaction,
            improvement = feedback.improvement,
            registeredAt = feedback.createdAt,
        )
    }
}
