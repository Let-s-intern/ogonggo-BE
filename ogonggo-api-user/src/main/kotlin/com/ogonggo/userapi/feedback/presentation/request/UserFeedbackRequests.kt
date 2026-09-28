package com.ogonggo.userapi.feedback.presentation.request

import com.ogonggo.userapi.error.InvalidRequestFieldException
import com.ogonggo.userapi.feedback.business.CreateFeedbackCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

/** 두 문항 모두 선택이지만 하나 이상은 채워야 한다. 공백만 있는 문항은 비운 것으로 본다. */
data class CreateFeedbackRequest(
    @Schema(description = "이용 중 가장 만족스러운 점")
    @field:Size(max = 1000)
    val satisfaction: String?,
    @Schema(description = "아쉬운 점이나 개선됐으면 하는 점")
    @field:Size(max = 1000)
    val improvement: String?,
) {
    fun toCommand(): CreateFeedbackCommand {
        val satisfaction = satisfaction?.takeIf { it.isNotBlank() }
        val improvement = improvement?.takeIf { it.isNotBlank() }
        if (satisfaction == null && improvement == null) {
            throw InvalidRequestFieldException("satisfaction", "만족스러운 점과 아쉬운 점 중 하나 이상 입력해 주세요.")
        }
        return CreateFeedbackCommand(satisfaction = satisfaction, improvement = improvement)
    }
}
