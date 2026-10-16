package com.ogonggo.adminapi.job.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "AI 서버 채용공고 등록 결과")
data class AiJobRegistrationResponse(
    @field:Schema(description = "등록된 채용공고 식별자")
    val jobId: Long,
)
