package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.user.business.UserAccountService
import com.ogonggo.userapi.user.presentation.request.ReplaceMyCompanyBasicInfoRequest
import com.ogonggo.userapi.user.presentation.request.ReplaceMyCompanyManagerInfoRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/users/me/company-profile")
class CompanyProfileController(
    private val userAccountService: UserAccountService,
) : CompanyProfileApi {

    @PutMapping("/basic-info")
    override fun replaceMyCompanyBasicInfo(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: ReplaceMyCompanyBasicInfoRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userAccountService.replaceMyCompanyBasicInfo(userId, request.toCommand())
        return SuccessResponse.ok()
    }

    @PutMapping("/manager-info")
    override fun replaceMyCompanyManagerInfo(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: ReplaceMyCompanyManagerInfoRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userAccountService.replaceMyCompanyManagerInfo(userId, request.toCommand())
        return SuccessResponse.ok()
    }
}
