package com.ogonggo.userapi.auth.presentation

import com.ogonggo.userapi.auth.business.UserAuthService
import com.ogonggo.userapi.auth.presentation.request.LetsCareerSignInRequest
import com.ogonggo.userapi.auth.presentation.request.TokenReissueRequest
import com.ogonggo.userapi.auth.presentation.response.AccessTokenResponse
import com.ogonggo.userapi.auth.presentation.response.SignInResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class UserAuthController(
    private val userAuthService: UserAuthService,
) : UserAuthApi {

    override fun signInWithLetsCareer(
        request: LetsCareerSignInRequest,
    ): ResponseEntity<SuccessResponse<SignInResponse>> =
        SuccessResponse.ok(SignInResponse.from(userAuthService.signInWithLetsCareer(request.letsCareerAccessToken)))

    override fun reissueAccessToken(
        request: TokenReissueRequest,
    ): ResponseEntity<SuccessResponse<AccessTokenResponse>> =
        SuccessResponse.ok(AccessTokenResponse(userAuthService.reissueAccessToken(request.refreshToken)))

    override fun signOut(userId: Long): ResponseEntity<SuccessResponse<Unit>> {
        userAuthService.signOut(userId)
        return SuccessResponse.ok()
    }
}
