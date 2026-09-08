package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.user.business.UserAccountService
import com.ogonggo.userapi.user.presentation.response.MyAccountResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/users/me")
class UserAccountController(
    private val userAccountService: UserAccountService,
) : UserAccountApi {

    @GetMapping
    override fun getMyAccount(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<SuccessResponse<MyAccountResponse>> =
        SuccessResponse.ok(MyAccountResponse.from(userAccountService.getMyAccount(userId)))
}
