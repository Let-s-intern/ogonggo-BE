package com.ogonggo.userapi.auth.presentation.response

import com.ogonggo.userapi.auth.business.AuthTokens

data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
) {
    companion object {
        internal fun from(tokens: AuthTokens): AuthTokenResponse = AuthTokenResponse(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
        )
    }
}
