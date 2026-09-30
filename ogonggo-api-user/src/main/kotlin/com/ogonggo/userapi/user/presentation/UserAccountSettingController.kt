package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.user.business.UserAccountService
import com.ogonggo.userapi.user.business.UserPasswordService
import com.ogonggo.userapi.user.presentation.request.ChangeMyPasswordRequest
import com.ogonggo.userapi.user.presentation.request.ReplaceMyNotificationEmailRequest
import com.ogonggo.userapi.user.presentation.request.ReplaceMyProfileImageRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/users/me")
class UserAccountSettingController(
    private val userAccountService: UserAccountService,
    private val userPasswordService: UserPasswordService,
) : UserAccountSettingApi {

    @PutMapping("/notification-email")
    override fun replaceMyNotificationEmail(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: ReplaceMyNotificationEmailRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userAccountService.changeMyNotificationEmail(userId, request.toNotificationEmail())
        return SuccessResponse.ok()
    }

    @PutMapping("/profile-image")
    override fun replaceMyProfileImage(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: ReplaceMyProfileImageRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userAccountService.replaceMyProfileImage(userId, request.imageId)
        return SuccessResponse.ok()
    }

    @DeleteMapping("/profile-image")
    override fun deleteMyProfileImage(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userAccountService.deleteMyProfileImage(userId)
        return SuccessResponse.ok()
    }

    @PatchMapping("/password")
    override fun changeMyPassword(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: ChangeMyPasswordRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        userPasswordService.changeMyPassword(userId, request.toCommand())
        return SuccessResponse.ok()
    }
}
