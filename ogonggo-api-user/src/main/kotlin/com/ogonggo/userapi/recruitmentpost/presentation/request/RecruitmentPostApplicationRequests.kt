package com.ogonggo.userapi.recruitmentpost.presentation.request

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationProgressStatus
import jakarta.validation.constraints.NotNull

data class UpdateRecruitmentPostApplicationStatusRequest(
    @field:NotNull
    val applicationStatus: RecruitmentPostApplicationProgressStatus?,
)
