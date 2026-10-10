package com.ogonggo.userapi.notification.fcm.presentation

import com.ogonggo.userapi.notification.fcm.business.FcmTokenService
import com.ogonggo.userapi.notification.fcm.presentation.request.ReplaceFcmTokenRequest
import com.ogonggo.userapi.notification.fcm.presentation.response.FcmTokenResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me/fcm-token")
class FcmTokenController(
    private val fcmTokenService: FcmTokenService,
) : FcmTokenApi {

    @PutMapping
    override fun replaceMyFcmToken(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: ReplaceFcmTokenRequest,
    ): ResponseEntity<SuccessResponse<FcmTokenResponse>> {
        fcmTokenService.replace(userId, request.token)
        return SuccessResponse.ok(FcmTokenResponse(request.token))
    }

    @GetMapping
    override fun getMyFcmToken(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<SuccessResponse<FcmTokenResponse>> =
        SuccessResponse.ok(FcmTokenResponse(fcmTokenService.get(userId)))
}
