package com.ogonggo.adminapi.job.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "원문 URL로 찾은 AI 서버 채용공고")
data class AiJobLookupResponse(
    @field:Schema(description = "채용공고 식별자")
    val jobId: Long,
)
