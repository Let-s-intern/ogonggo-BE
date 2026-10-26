package com.ogonggo.adminapi.notification.intake.presentation.response

import com.ogonggo.adminapi.notification.intake.business.AdminWelcomeEmailResendResult
import java.time.LocalDateTime

data class AdminWelcomeEmailResendResponse(
    val notificationId: Long,
    val recipientUserId: Long,
    val templateCode: String,
    val scheduledAt: LocalDateTime,
) {
    companion object {
        fun from(result: AdminWelcomeEmailResendResult) = AdminWelcomeEmailResendResponse(
            notificationId = result.notificationId,
            recipientUserId = result.recipientUserId,
            templateCode = result.templateCode,
            scheduledAt = result.scheduledAt,
        )
    }
}
