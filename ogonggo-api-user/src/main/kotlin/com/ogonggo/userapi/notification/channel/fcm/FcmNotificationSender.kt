package com.ogonggo.userapi.notification.channel.fcm

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.core.notification.domain.NotificationRecipientType
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenManager
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenReader
import com.ogonggo.userapi.notification.delivery.implement.NotificationSender
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

/** FCM 알림 행을 Firebase Admin SDK 메시지로 변환해 발송한다. */
@Component
@ConditionalOnBean(FirebaseMessaging::class)
internal class FcmNotificationSender(
    private val firebaseMessaging: FirebaseMessaging,
    private val objectMapper: ObjectMapper,
    private val tokenReader: UserFcmTokenReader,
    private val tokenManager: UserFcmTokenManager,
    private val clock: Clock,
) : NotificationSender {

    override val channel = NotificationChannel.FCM

    override fun send(notification: NotificationMessageDto): NotificationDeliveryResult {
        val payload = try {
            objectMapper.readValue(notification.payloadJson, FcmNotificationPayload::class.java)
        } catch (_: Exception) {
            return NotificationDeliveryResult.Failed(
                resultCode = "INVALID_FCM_PAYLOAD",
                failureCategory = NotificationFailureCategory.INVALID_REQUEST,
                detail = "FCM payload를 해석할 수 없습니다.",
            )
        }

        val userId = notification.recipientUserId
        val topic = notification.recipientType == NotificationRecipientType.TOPIC
        if (!topic && (userId == null || !tokenReader.isDeliverable(userId, notification.recipientAddress))) {
            return NotificationDeliveryResult.Failed(
                resultCode = "FCM_RECIPIENT_INACTIVE",
                failureCategory = NotificationFailureCategory.CLIENT_ERROR,
                detail = "수신자가 비활성이거나 토큰 연결이 해제·변경되었습니다.",
            )
        }

        val message = Message.builder()
            .apply { if (topic) setTopic(notification.recipientAddress) else setToken(notification.recipientAddress) }
            .setNotification(
                Notification.builder()
                    .setTitle(payload.title)
                    .setBody(payload.body)
                    .build(),
            )
            .putAllData(payload.data)
            .build()

        return try {
            NotificationDeliveryResult.Sent(firebaseMessaging.send(message))
        } catch (exception: FirebaseMessagingException) {
            val errorCode = exception.messagingErrorCode?.name ?: "FIREBASE_MESSAGING_ERROR"
            if (!topic && userId != null && exception.messagingErrorCode == com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED) {
                tokenManager.revoke(userId, notification.recipientAddress, LocalDateTime.now(clock))
            }
            NotificationDeliveryResult.Failed(
                resultCode = errorCode,
                failureCategory = errorCode.toFailureCategory(),
                detail = exception.message.toSafeDetail(notification.recipientAddress),
            )
        }
    }

    private fun String?.toSafeDetail(token: String): String? = this
        ?.replace(FCM_TOKEN_PATTERN, "[REDACTED]")
        ?.replace(token, "[REDACTED]")
        ?.replace(EMAIL_PATTERN, "[REDACTED]")
        ?.replace(PHONE_PATTERN, "[REDACTED]")
        ?.replace(WHITESPACE_PATTERN, " ")
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.take(RESULT_DETAIL_MAX_LENGTH)

    private fun String.toFailureCategory(): NotificationFailureCategory = when (this) {
        "UNREGISTERED", "INVALID_ARGUMENT" -> NotificationFailureCategory.CLIENT_ERROR
        "SENDER_ID_MISMATCH", "THIRD_PARTY_AUTH_ERROR" -> NotificationFailureCategory.AUTHENTICATION_CONFIGURATION
        "QUOTA_EXCEEDED" -> NotificationFailureCategory.RATE_LIMITED
        "UNAVAILABLE" -> NotificationFailureCategory.PROVIDER_UNAVAILABLE
        else -> NotificationFailureCategory.HTTP_ERROR
    }

    private companion object {
        private const val RESULT_DETAIL_MAX_LENGTH = 500
        private val EMAIL_PATTERN = Regex("(?i)\\b[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}\\b")
        private val PHONE_PATTERN = Regex("\\b\\d{9,15}\\b")
        private val FCM_TOKEN_PATTERN = Regex("\\b[A-Za-z0-9_-]{20,}:[A-Za-z0-9_:-]{10,}\\b")
        private val WHITESPACE_PATTERN = Regex("\\s+")
    }
}

internal data class FcmNotificationPayload(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
)
