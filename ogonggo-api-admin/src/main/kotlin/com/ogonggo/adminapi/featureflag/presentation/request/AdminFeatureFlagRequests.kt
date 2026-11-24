package com.ogonggo.adminapi.featureflag.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class UpdateFeatureFlagRequest(
    @field:Schema(description = "참이면 플래그를 켠다")
    @field:NotNull
    val enabled: Boolean?,
)
