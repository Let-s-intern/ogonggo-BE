package com.ogonggo.adminapi.notification.intake.presentation

import com.ogonggo.adminapi.notification.intake.business.AdminWelcomeEmailService
import com.ogonggo.adminapi.notification.intake.presentation.response.AdminWelcomeEmailResendResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/general-members/{userId}/welcome-email-resends")
class AdminWelcomeEmailController(
    private val adminWelcomeEmailService: AdminWelcomeEmailService,
) : AdminWelcomeEmailApi {

    @PostMapping
    override fun createWelcomeEmailResend(
        @AuthenticationPrincipal adminUserId: Long,
        @PathVariable("userId") userId: Long,
    ): ResponseEntity<SuccessResponse<AdminWelcomeEmailResendResponse>> {
        val result = adminWelcomeEmailService.createResend(adminUserId, userId)
        log.info(
            "가입 환영 메일 재발송 적재 완료. notificationId={}, adminUserId={}, recipientUserId={}",
            result.notificationId, adminUserId, userId,
        )
        return SuccessResponse.created(AdminWelcomeEmailResendResponse.from(result))
    }

    private companion object {
        val log = LoggerFactory.getLogger(AdminWelcomeEmailController::class.java)
    }
}
