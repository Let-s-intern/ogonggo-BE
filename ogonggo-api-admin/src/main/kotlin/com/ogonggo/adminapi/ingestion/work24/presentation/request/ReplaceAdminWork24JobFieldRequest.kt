package com.ogonggo.adminapi.ingestion.work24.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class ReplaceAdminWork24JobFieldRequest(
    @field:Schema(description = "참이면 고용24 채용정보 수집에서 이 직군의 공고를 받는다")
    @field:NotNull
    val enabled: Boolean?,
)
