package com.ogonggo.userapi.usernotification.presentation

import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.usernotification.business.UserNotificationService
import com.ogonggo.userapi.usernotification.presentation.response.UserNotificationPageResponse
import com.ogonggo.userapi.usernotification.presentation.response.UserNotificationUnreadCountResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
@RequestMapping("/api/v1/users/me/notifications")
class UserNotificationController(private val service: UserNotificationService) : UserNotificationApi {
    @GetMapping
    override fun listMyNotifications(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(name = "cursor", required = false) cursor: String?,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<SuccessResponse<UserNotificationPageResponse>> =
        SuccessResponse.ok(UserNotificationPageResponse.from(service.list(userId, cursor, size)))

    @GetMapping("/unread-count")
    override fun getMyNotificationUnreadCount(@AuthenticationPrincipal userId: Long): ResponseEntity<SuccessResponse<UserNotificationUnreadCountResponse>> =
        SuccessResponse.ok(UserNotificationUnreadCountResponse(service.countUnread(userId)))

    @PatchMapping("/{notificationId}/read")
    override fun readMyNotification(
        @AuthenticationPrincipal userId: Long,
        @PathVariable(name = "notificationId") notificationId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        service.markRead(userId, notificationId)
        return SuccessResponse.ok()
    }

    @PatchMapping("/broadcasts/{batchId}/read")
    override fun readMyBroadcastNotification(
        @AuthenticationPrincipal userId: Long,
        @PathVariable(name = "batchId") batchId: java.util.UUID,
    ): ResponseEntity<SuccessResponse<Unit>> {
        service.markBroadcastRead(userId, batchId)
        return SuccessResponse.ok()
    }

    @PatchMapping("/read-all")
    override fun readAllMyNotifications(@AuthenticationPrincipal userId: Long): ResponseEntity<SuccessResponse<Unit>> {
        service.markAllRead(userId)
        return SuccessResponse.ok()
    }
}
