package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.user.business.UserAccountService
import com.ogonggo.userapi.user.presentation.request.ReplaceMyProfileRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/users/me/profile")
class UserProfileController(
    private val userAccountService: UserAccountService,
) : UserProfileApi {

    @PutMapping
    override fun replaceMyProfile(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: ReplaceMyProfileRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userAccountService.replaceMyProfile(userId, request.toCommand())
        return SuccessResponse.ok()
    }
}
