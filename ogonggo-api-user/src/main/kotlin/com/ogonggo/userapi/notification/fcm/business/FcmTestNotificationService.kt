package com.ogonggo.userapi.notification.fcm.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenReader
import com.ogonggo.userapi.user.implement.requireActive
import com.ogonggo.userapi.notification.fcm.error.FcmNotificationErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/** 저장된 본인 FCM 토큰으로 provider 발송 경로를 검증하는 수동 테스트 기능이다. */
@Service
class FcmTestNotificationService(
    private val userReader: UserReader,
    private val tokenReader: UserFcmTokenReader,
    private val notificationAppender: NotificationAppender,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {

    @Transactional
    fun send(userId: Long, command: SendFcmTestNotificationCommand): String {
        userReader.read(userId).status.requireActive()
        val token = tokenReader.readLatest(userId)
            ?: throw EntityNotFoundException(FcmNotificationErrorCode.FCM_TOKEN_NOT_REGISTERED)
        val deduplicationKey = "fcm-test:$userId:${UUID.randomUUID()}"
        val scheduledAt = LocalDateTime.now(clock)

        notificationAppender.append(
            NotificationAppendDto(
                deduplicationKey = deduplicationKey,
                channel = NotificationChannel.FCM,
                templateCode = "fcm_test",
                recipientUserId = userId,
                recipientAddress = token,
                payloadJson = objectMapper.writeValueAsString(
                    mapOf(
                        "title" to command.title,
                        "body" to command.body,
                        "data" to command.data,
                    ),
                ),
                scheduledAt = scheduledAt,
            ),
        )
        return deduplicationKey
    }
}
