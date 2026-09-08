package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.user.business.UserAccountService
import com.ogonggo.userapi.user.presentation.response.MyAccountResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class UserAccountController(
    private val userAccountService: UserAccountService,
) : UserAccountApi {

    override fun getMyAccount(userId: Long): ResponseEntity<SuccessResponse<MyAccountResponse>> =
        SuccessResponse.ok(MyAccountResponse.from(userAccountService.getMyAccount(userId)))
}
