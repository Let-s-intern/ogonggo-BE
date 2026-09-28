package com.ogonggo.adminapi.servicefeedback.presentation.response

import com.ogonggo.adminapi.servicefeedback.business.AdminServiceFeedbackSummary
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminServiceFeedbackResponse(
    val id: Long,
    @Schema(description = "작성한 사용자 식별자. 로그인 없이 작성했으면 null입니다.")
    val userId: Long?,
    @Schema(description = "이용 중 가장 만족스러운 점. 비워 두었으면 null입니다.")
    val satisfaction: String?,
    @Schema(description = "아쉬운 점이나 개선됐으면 하는 점. 비워 두었으면 null입니다.")
    val improvement: String?,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminServiceFeedbackSummary): AdminServiceFeedbackResponse = AdminServiceFeedbackResponse(
            id = result.id,
            userId = result.userId,
            satisfaction = result.satisfaction,
            improvement = result.improvement,
            registeredAt = result.registeredAt,
        )
    }
}
