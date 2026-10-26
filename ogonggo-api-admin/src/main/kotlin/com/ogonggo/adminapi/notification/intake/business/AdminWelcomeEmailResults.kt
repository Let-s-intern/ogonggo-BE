package com.ogonggo.adminapi.notification.intake.business

import java.time.LocalDateTime

data class AdminWelcomeEmailResendResult(
    val notificationId: Long,
    val recipientUserId: Long,
    val templateCode: String,
    val scheduledAt: LocalDateTime,
)
