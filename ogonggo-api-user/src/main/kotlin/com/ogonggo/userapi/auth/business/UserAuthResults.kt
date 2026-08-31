package com.ogonggo.userapi.auth.business

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

data class SignInResult(
    val tokens: AuthTokens,
    val isNewUser: Boolean,
)
