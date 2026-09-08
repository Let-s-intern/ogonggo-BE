package com.ogonggo.userapi.auth.presentation

import com.ogonggo.userapi.auth.business.UserAuthService
import com.ogonggo.userapi.auth.presentation.request.LetsCareerSignInRequest
import com.ogonggo.userapi.auth.presentation.request.TokenReissueRequest
import com.ogonggo.userapi.auth.presentation.response.AccessTokenResponse
import com.ogonggo.userapi.auth.presentation.response.SignInResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class UserAuthController(
    private val userAuthService: UserAuthService,
) : UserAuthApi {

    @PostMapping("/letscareer")
    override fun signInWithLetsCareer(
        @RequestBody request: LetsCareerSignInRequest,
    ): ResponseEntity<SuccessResponse<SignInResponse>> =
        SuccessResponse.ok(SignInResponse.from(userAuthService.signInWithLetsCareer(request.letsCareerAccessToken)))

    @PostMapping("/token")
    override fun reissueAccessToken(
        @RequestBody request: TokenReissueRequest,
    ): ResponseEntity<SuccessResponse<AccessTokenResponse>> =
        SuccessResponse.ok(AccessTokenResponse(userAuthService.reissueAccessToken(request.refreshToken)))

    @PostMapping("/signout")
    override fun signOut(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userAuthService.signOut(userId)
        return SuccessResponse.ok()
    }
}
