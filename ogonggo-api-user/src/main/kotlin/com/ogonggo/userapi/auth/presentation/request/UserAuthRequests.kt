package com.ogonggo.userapi.auth.presentation.request

import jakarta.validation.constraints.NotBlank

data class LetsCareerSignInRequest(
    @field:NotBlank val letsCareerAccessToken: String,
)

data class TokenReissueRequest(
    @field:NotBlank val refreshToken: String,
)
