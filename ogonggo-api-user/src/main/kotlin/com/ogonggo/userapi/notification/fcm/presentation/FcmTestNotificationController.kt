package com.ogonggo.userapi.notification.fcm.presentation

import com.ogonggo.userapi.notification.fcm.business.FcmTestNotificationService
import com.ogonggo.userapi.notification.fcm.presentation.request.SendFcmTestNotificationRequest
import com.ogonggo.userapi.notification.fcm.presentation.response.FcmTestNotificationResponse
import com.ogonggo.userapi.response.SuccessResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me/notifications/fcm/test")
class FcmTestNotificationController(
    private val fcmTestNotificationService: FcmTestNotificationService,
) : FcmTestNotificationApi {

    @PostMapping
    override fun sendMyFcmTestNotification(
        @AuthenticationPrincipal userId: Long,
        @Valid
        @RequestBody request: SendFcmTestNotificationRequest,
    ): ResponseEntity<SuccessResponse<FcmTestNotificationResponse>> =
        SuccessResponse.ok(
            FcmTestNotificationResponse(
                deduplicationKey = fcmTestNotificationService.send(userId, request.toCommand()),
            ),
        )
}
