package com.ogonggo.adminapi.companycontentpriority.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class ReplaceAdminCompanyContentPriorityRequest(
    @field:Schema(description = "참이면 마감 전 기업회원 공고·부트캠프를 공개 목록 맨 앞에 둔다")
    @field:NotNull
    val enabled: Boolean?,
)
