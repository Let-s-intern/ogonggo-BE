package com.ogonggo.adminapi.notification.intake.business

import com.ogonggo.adminapi.notification.intake.error.AdminWelcomeEmailErrorCode
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class AdminWelcomeEmailService(
    private val userReader: UserReader,
    private val userProfileReader: UserProfileReader,
    private val notificationAppender: NotificationAppender,
    private val clock: Clock,
) {

    /** 기존 발송 결과를 보존하고 현재 가입 이메일로 별도 회차를 적재한다. */
    @Transactional
    fun createResend(adminUserId: Long, userId: Long): AdminWelcomeEmailResendResult {
        val user = userReader.read(userId)
        if (user.letsCareerUserId == null) {
            throw ForbiddenException(UserErrorCode.GENERAL_MEMBER_REQUIRED)
        }
        when (user.status) {
            UserStatus.SUSPENDED -> throw ForbiddenException(UserErrorCode.USER_SUSPENDED)
            UserStatus.WITHDRAWN -> throw ForbiddenException(UserErrorCode.USER_WITHDRAWN)
            UserStatus.ACTIVE -> Unit
        }
        val email = userProfileReader.read(userId)?.email?.takeIf(String::isNotBlank)
            ?: throw ConflictException(AdminWelcomeEmailErrorCode.WELCOME_EMAIL_RECIPIENT_UNAVAILABLE)
        val now = LocalDateTime.now(clock)
        val notificationId = checkNotNull(notificationAppender.appendAndGetId(
            NotificationAppendDto(
                deduplicationKey = "$TEMPLATE_CODE:user:$userId:EMAIL:admin:$adminUserId:resend:${UUID.randomUUID()}",
                channel = NotificationChannel.EMAIL,
                templateCode = TEMPLATE_CODE,
                recipientAddress = email,
                payloadJson = "{}",
                scheduledAt = now,
                recipientUserId = userId,
                dispatchExpiresAt = now.plusHours(24),
            ),
        ))
        return AdminWelcomeEmailResendResult(notificationId, userId, TEMPLATE_CODE, now)
    }

    private companion object {
        /** 사용자 API의 EmailTemplate.SIGN_UP_WELCOME가 처리하는 적재 계약이다. */
        const val TEMPLATE_CODE = "sign_up_welcome"
    }
}
