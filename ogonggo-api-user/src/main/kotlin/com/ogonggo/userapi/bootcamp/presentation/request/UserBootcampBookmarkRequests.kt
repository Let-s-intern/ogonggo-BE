package com.ogonggo.userapi.bootcamp.presentation.request

import com.ogonggo.core.bootcamp.domain.BootcampApplicationStatus
import jakarta.validation.constraints.NotNull

data class UpdateBootcampApplicationStatusRequest(
    @field:NotNull
    val applicationStatus: BootcampApplicationStatus?,
)
