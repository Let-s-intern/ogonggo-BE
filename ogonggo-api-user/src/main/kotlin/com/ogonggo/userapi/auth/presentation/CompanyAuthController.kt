package com.ogonggo.userapi.auth.presentation

import com.ogonggo.userapi.auth.business.CompanyAuthService
import com.ogonggo.userapi.auth.presentation.request.CompanySignInRequest
import com.ogonggo.userapi.auth.presentation.request.CompanySignUpRequest
import com.ogonggo.userapi.auth.presentation.response.AuthTokenResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CompanyAuthController(
    private val companyAuthService: CompanyAuthService,
) : CompanyAuthApi {

    override fun signUp(request: CompanySignUpRequest): ResponseEntity<SuccessResponse<AuthTokenResponse>> =
        SuccessResponse.created(AuthTokenResponse.from(companyAuthService.signUp(request.toCommand())))

    override fun signIn(request: CompanySignInRequest): ResponseEntity<SuccessResponse<AuthTokenResponse>> =
        SuccessResponse.ok(AuthTokenResponse.from(companyAuthService.signIn(request.toCommand())))
}
