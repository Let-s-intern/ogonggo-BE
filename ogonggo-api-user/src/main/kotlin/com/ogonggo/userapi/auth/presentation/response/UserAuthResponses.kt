package com.ogonggo.userapi.auth.presentation.response

import com.ogonggo.userapi.auth.business.SignInResult

data class SignInResponse(
    val accessToken: String,
    val refreshToken: String,
    val isNewUser: Boolean,
) {
    companion object {
        internal fun from(result: SignInResult): SignInResponse = SignInResponse(
            accessToken = result.tokens.accessToken,
            refreshToken = result.tokens.refreshToken,
            isNewUser = result.isNewUser,
        )
    }
}

data class AccessTokenResponse(
    val accessToken: String,
)
