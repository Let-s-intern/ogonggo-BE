package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.user.business.UserAccountService
import com.ogonggo.userapi.user.presentation.request.ReplaceMyProfileRequest
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class UserProfileController(
    private val userAccountService: UserAccountService,
) : UserProfileApi {

    override fun replaceMyProfile(
        userId: Long,
        request: ReplaceMyProfileRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userAccountService.replaceMyProfile(userId, request.toCommand())
        return SuccessResponse.ok()
    }
}
