package com.ogonggo.userapi.notification.channel.fcm

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
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

        val message = try {
            Message.builder()
                .apply { if (topic) setTopic(notification.recipientAddress) else setToken(notification.recipientAddress) }
                .setNotification(
                    Notification.builder()
                        .setTitle(payload.title)
                        .setBody(payload.body)
                        .build(),
                )
                .putAllData(payload.data)
                .build()
        } catch (_: IllegalArgumentException) {
            return NotificationDeliveryResult.Failed(
                resultCode = "INVALID_FCM_MESSAGE",
                failureCategory = NotificationFailureCategory.INVALID_REQUEST,
                detail = "FCM 메시지 형식이 유효하지 않습니다.",
            )
        }

        return try {
            NotificationDeliveryResult.Sent(firebaseMessaging.send(message))
        } catch (exception: FirebaseMessagingException) {
            if (!topic && userId != null && exception.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
                tokenManager.revoke(userId, notification.recipientAddress, LocalDateTime.now(clock))
            }
            exception.toDeliveryResult(notification.recipientAddress)
        }
    }

    private fun FirebaseMessagingException.toDeliveryResult(token: String): NotificationDeliveryResult {
        // 통신 오류에는 messagingErrorCode가 없으므로 DEADLINE_EXCEEDED 등 SDK 공통 코드도 확인한다.
        val resultCode = messagingErrorCode?.name ?: errorCode?.name ?: "FIREBASE_MESSAGING_ERROR"
        val category = resultCode.toFailureCategory()
        val detail = message.toSafeDetail(token)
        return when (category) {
            NotificationFailureCategory.CLIENT_ERROR,
            NotificationFailureCategory.INVALID_REQUEST,
            NotificationFailureCategory.AUTHENTICATION_CONFIGURATION,
            NotificationFailureCategory.RATE_LIMITED,
            NotificationFailureCategory.HTTP_ERROR -> NotificationDeliveryResult.Failed(resultCode, category, detail)

            else -> NotificationDeliveryResult.Unknown(resultCode, category, detail)
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
        "UNREGISTERED" -> NotificationFailureCategory.CLIENT_ERROR
        "INVALID_ARGUMENT", "FAILED_PRECONDITION", "OUT_OF_RANGE" -> NotificationFailureCategory.INVALID_REQUEST
        "SENDER_ID_MISMATCH", "THIRD_PARTY_AUTH_ERROR", "UNAUTHENTICATED", "PERMISSION_DENIED" ->
            NotificationFailureCategory.AUTHENTICATION_CONFIGURATION
        "QUOTA_EXCEEDED", "RESOURCE_EXHAUSTED" -> NotificationFailureCategory.RATE_LIMITED
        "INTERNAL", "UNAVAILABLE" -> NotificationFailureCategory.PROVIDER_UNAVAILABLE
        "DEADLINE_EXCEEDED", "CANCELLED" -> NotificationFailureCategory.TRANSPORT_ERROR
        "NOT_FOUND", "CONFLICT", "ALREADY_EXISTS" -> NotificationFailureCategory.HTTP_ERROR
        else -> NotificationFailureCategory.UNKNOWN_PROVIDER_ERROR
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
