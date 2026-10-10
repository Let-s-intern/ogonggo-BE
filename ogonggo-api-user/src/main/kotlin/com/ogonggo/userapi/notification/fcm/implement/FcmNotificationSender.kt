package com.ogonggo.userapi.notification.fcm.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.user.implement.UserManager
import com.ogonggo.userapi.notification.delivery.implement.NotificationSender
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component

/** FCM 알림 행을 Firebase Admin SDK 메시지로 변환해 발송한다. */
@Component
@ConditionalOnBean(FirebaseMessaging::class)
internal class FcmNotificationSender(
    private val firebaseMessaging: FirebaseMessaging,
    private val objectMapper: ObjectMapper,
    private val userManager: UserManager,
) : NotificationSender {

    override val channel = NotificationChannel.FCM

    override fun send(notification: NotificationMessageDto): NotificationDeliveryResult {
        val payload = try {
            objectMapper.readValue(notification.payloadJson, FcmNotificationPayload::class.java)
        } catch (_: Exception) {
            return NotificationDeliveryResult.Failed(
                resultCode = "INVALID_FCM_PAYLOAD",
                failureCategory = NotificationFailureCategory.INVALID_REQUEST,
            )
        }

        val message = Message.builder()
            .setToken(notification.recipientAddress)
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
            if (exception.messagingErrorCode == com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED) {
                notification.recipientUserId?.let { userManager.clearFcmToken(it, notification.recipientAddress) }
            }
            NotificationDeliveryResult.Failed(errorCode, errorCode.toFailureCategory())
        }
    }

    private fun String.toFailureCategory(): NotificationFailureCategory = when (this) {
        "UNREGISTERED", "INVALID_ARGUMENT" -> NotificationFailureCategory.CLIENT_ERROR
        "SENDER_ID_MISMATCH", "THIRD_PARTY_AUTH_ERROR" -> NotificationFailureCategory.AUTHENTICATION_CONFIGURATION
        "QUOTA_EXCEEDED" -> NotificationFailureCategory.RATE_LIMITED
        "UNAVAILABLE" -> NotificationFailureCategory.PROVIDER_UNAVAILABLE
        else -> NotificationFailureCategory.HTTP_ERROR
    }
}

internal data class FcmNotificationPayload(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
)
