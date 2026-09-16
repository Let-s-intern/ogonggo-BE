package com.ogonggo.userapi.community.presentation.request

import jakarta.validation.constraints.Size

data class CreateRecruitmentPostCommentReportRequest(
    @field:Size(max = 500)
    val reason: String? = null,
)
