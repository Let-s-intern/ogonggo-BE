package com.ogonggo.userapi.job.presentation.request

import com.ogonggo.core.job.domain.JobApplicationStatus
import jakarta.validation.constraints.NotNull

data class UpdateJobApplicationStatusRequest(
    @field:NotNull
    val applicationStatus: JobApplicationStatus?,
)
