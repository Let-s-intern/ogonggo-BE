package com.ogonggo.adminapi.servicefeedback.business

import com.ogonggo.core.servicefeedback.domain.ServiceFeedback
import com.ogonggo.core.servicefeedback.implement.dto.ServiceFeedbackPageDto
import com.ogonggo.core.user.implement.dto.UserProfileDto
import java.time.LocalDateTime

data class AdminServiceFeedbackPageResult(
    val items: List<AdminServiceFeedbackSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(
            result: ServiceFeedbackPageDto,
            profiles: Map<Long, UserProfileDto>,
        ): AdminServiceFeedbackPageResult = AdminServiceFeedbackPageResult(
            items = result.serviceFeedbacks.map { serviceFeedback ->
                AdminServiceFeedbackSummary.from(serviceFeedback, serviceFeedback.userId?.let(profiles::get))
            },
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
    val userName: String?,
    val userNickname: String?,
    val userEmail: String?,
    val satisfaction: String?,
    val improvement: String?,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(
            serviceFeedback: ServiceFeedback,
            userProfile: UserProfileDto?,
        ): AdminServiceFeedbackSummary = AdminServiceFeedbackSummary(
            id = checkNotNull(serviceFeedback.id) { "개선 의견 식별자가 없습니다." },
            userId = serviceFeedback.userId,
            userName = userProfile?.name,
            userNickname = userProfile?.nickname,
            userEmail = userProfile?.email,
            satisfaction = serviceFeedback.satisfaction,
            improvement = serviceFeedback.improvement,
            registeredAt = serviceFeedback.createdAt,
        )
    }
}
