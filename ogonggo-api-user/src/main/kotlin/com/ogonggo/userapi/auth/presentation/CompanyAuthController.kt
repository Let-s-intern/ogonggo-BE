package com.ogonggo.userapi.auth.presentation

import com.ogonggo.userapi.auth.business.CompanyAuthService
import com.ogonggo.userapi.auth.presentation.request.CompanySignInRequest
import com.ogonggo.userapi.auth.presentation.request.CompanySignUpRequest
import com.ogonggo.userapi.auth.presentation.response.AuthTokenResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth/company")
class CompanyAuthController(
    private val companyAuthService: CompanyAuthService,
) : CompanyAuthApi {

    @PostMapping("/signup")
    override fun signUp(
        @RequestBody request: CompanySignUpRequest,
    ): ResponseEntity<SuccessResponse<AuthTokenResponse>> =
        SuccessResponse.created(AuthTokenResponse.from(companyAuthService.signUp(request.toCommand())))

    @PostMapping("/signin")
    override fun signIn(
        @RequestBody request: CompanySignInRequest,
    ): ResponseEntity<SuccessResponse<AuthTokenResponse>> =
        SuccessResponse.ok(AuthTokenResponse.from(companyAuthService.signIn(request.toCommand())))
}
