package com.ogonggo.userapi.community.presentation.request

import com.ogonggo.core.community.domain.RecruitmentApplicationProgressStatus
import jakarta.validation.constraints.NotNull

data class UpdateRecruitmentApplicationStatusRequest(
    @field:NotNull
    val applicationStatus: RecruitmentApplicationProgressStatus?,
)
