package com.ogonggo.userapi.recruitmentpost.presentation.request

import com.ogonggo.userapi.recruitmentpost.business.CreateRecruitmentPostCommentCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateRecruitmentPostCommentRequest(
    @field:NotBlank
    @field:Size(max = 1000)
    val content: String,
    @field:Positive
    val parentId: Long? = null,
) {
    fun toCommand(): CreateRecruitmentPostCommentCommand = CreateRecruitmentPostCommentCommand(
        parentId = parentId,
        content = content,
    )
}
