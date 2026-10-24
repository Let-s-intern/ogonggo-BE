package com.ogonggo.adminapi.notificationbatch.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.notification.domain.NotificationRecipientType
import com.ogonggo.core.notificationbatch.implement.NotificationBroadcastProperties
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType
import com.ogonggo.core.notificationbatch.implement.NotificationBatchAppender
import com.ogonggo.core.notificationbatch.implement.NotificationBatchReader
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchAppendDto
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchDto
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchResultDto
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenRecipientReader
import com.ogonggo.core.usernotification.domain.UserNotificationPushSkipReason
import com.ogonggo.core.usernotification.domain.UserNotificationType
import com.ogonggo.core.usernotification.implement.UserNotificationAppender
import com.ogonggo.core.usernotification.implement.dto.UserNotificationAppendDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class AdminNotificationBatchService(
    private val recipients: UserFcmTokenRecipientReader,
    private val notifications: NotificationAppender,
    private val batches: NotificationBatchAppender,
    private val batchReader: NotificationBatchReader,
    private val inbox: UserNotificationAppender,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
    private val broadcast: NotificationBroadcastProperties,
) {
    /** 알림함·발송 적재·요청 기록을 함께 커밋한다. provider 호출은 사용자 API의 기존 dispatcher가 맡는다. */
    @Transactional
    fun create(adminUserId: Long, command: CreateNotificationBatchCommand): NotificationBatchDto {
        val userIds = validateTargets(command)
        validateData(command.data)
        if (command.targetType == NotificationBatchTargetType.ALL && broadcast.enabled) {
            return createBroadcast(adminUserId, command)
        }
        notificationPayload(command, command.data)
        val previewLength = minOf(100, command.body.codePointCount(0, command.body.length))
        val preview = command.body.substring(0, command.body.offsetByCodePoints(0, previewLength))

        val now = LocalDateTime.now(clock)
        val expiresAt = now.plusHours(24)
        val batchId = UUID.randomUUID().toString()
        val throughUserId = recipients.readUpperUserId()
        var afterUserId = 0L
        var targetCount = 0L
        var recipientCount = 0L
        var enqueuedCount = 0L
        var noTokenCount = 0L
        var inactiveCount = 0L
        while (true) {
            val page = recipients.readPage(afterUserId, throughUserId, userIds, 500)
            if (page.isEmpty()) break
            targetCount += page.size
            val pending = page.flatMap { recipient ->
                when {
                    recipient.status != UserStatus.ACTIVE -> { inactiveCount++; emptyList() }
                    else -> {
                        val inboxId = checkNotNull(inbox.append(UserNotificationAppendDto(
                            recipientUserId = recipient.userId,
                            type = UserNotificationType.ADMIN_NOTIFICATION,
                            actorUserId = adminUserId, postId = null, commentId = null,
                            actorNickname = "오공고", actorProfileImageUrl = null,
                            postTitle = command.title, preview = preview,
                            pushSkipReason = if (recipient.tokens.isEmpty()) UserNotificationPushSkipReason.NO_ACTIVE_TOKEN else null,
                            batchId = batchId, url = command.data.getValue("url"),
                        )))
                        // 요청 data가 시스템의 알림함 ID·종류를 덮어쓰지 못하게 한다.
                        val data = (command.data - setOf("source", "batchId")) + mapOf("notificationId" to inboxId.toString(), "type" to UserNotificationType.ADMIN_NOTIFICATION.name)
                        val payload = notificationPayload(command, data)
                        if (recipient.tokens.isEmpty()) noTokenCount++ else recipientCount++
                        recipient.tokens.map { token -> NotificationAppendDto(
                            deduplicationKey = "admin-batch:$batchId:token:${token.id}",
                            channel = NotificationChannel.FCM, templateCode = "admin_notification",
                            recipientUserId = recipient.userId, recipientAddress = token.token,
                            payloadJson = payload, scheduledAt = now, batchId = batchId, dispatchExpiresAt = expiresAt,
                            userNotificationId = inboxId,
                        ) }
                    }
                }
            }
            enqueuedCount += notifications.appendAll(pending)
            afterUserId = page.last().userId
        }
        if (userIds != null && targetCount != userIds.size.toLong()) {
            // 이전 페이지를 적재했더라도 요청 전체가 롤백되어 부분 발송은 생기지 않는다.
            throw EntityNotFoundException(UserErrorCode.USER_NOT_FOUND)
        }
        return batches.append(NotificationBatchAppendDto(
            batchId = batchId, requestedByUserId = adminUserId, targetType = command.targetType,
            requestedUserIdsJson = userIds?.let { objectMapper.writeValueAsString(it.sorted()) },
            title = command.title, body = command.body, dataJson = objectMapper.writeValueAsString(command.data),
            targetUserCount = targetCount, recipientUserCount = recipientCount, enqueuedCount = enqueuedCount,
            skippedNoTokenUserCount = noTokenCount, skippedInactiveUserCount = inactiveCount, dispatchExpiresAt = expiresAt,
        ))
    }

    private fun createBroadcast(adminUserId: Long, command: CreateNotificationBatchCommand): NotificationBatchDto {
        val now = LocalDateTime.now(clock)
        val batchId = UUID.randomUUID().toString()
        val expiresAt = now.plusHours(24)
        val (count, noToken) = recipients.countActiveAudience()
        val preview = command.body.substring(0, command.body.offsetByCodePoints(0,
            minOf(100, command.body.codePointCount(0, command.body.length))))
        val data = (command.data - "notificationId") + mapOf("batchId" to batchId, "type" to "ADMIN_NOTIFICATION", "source" to "BROADCAST")
        val payload = notificationPayload(command.copy(body = preview), data, 2048)
        val batch = batches.append(NotificationBatchAppendDto(
            batchId = batchId, requestedByUserId = adminUserId, targetType = command.targetType,
            requestedUserIdsJson = null, title = command.title, body = command.body,
            dataJson = objectMapper.writeValueAsString(command.data), targetUserCount = count,
            recipientUserCount = count - noToken, enqueuedCount = 1, skippedNoTokenUserCount = noToken,
            skippedInactiveUserCount = 0, dispatchExpiresAt = expiresAt, topicName = broadcast.topic,
            url = command.data.getValue("url"),
        ))
        check(notifications.append(NotificationAppendDto(
            deduplicationKey = "admin-batch:$batchId:topic", channel = NotificationChannel.FCM,
            recipientType = NotificationRecipientType.TOPIC, templateCode = "admin_notification",
            recipientAddress = broadcast.topic, payloadJson = payload, scheduledAt = now,
            batchId = batchId, dispatchExpiresAt = expiresAt,
        )))
        return batch
    }

    fun get(batchId: UUID): NotificationBatchResultDto = batchReader.readResult(batchId.toString(), LocalDateTime.now(clock))

    private fun notificationPayload(command: CreateNotificationBatchCommand, data: Map<String, String>, maxBytes: Int = 4096): String {
        val messageBytes = objectMapper.writeValueAsBytes(mapOf(
            "notification" to mapOf("title" to command.title, "body" to command.body), "data" to data,
        ))
        if (messageBytes.size > maxBytes) throw InvalidRequestFieldException("data", "시스템 식별자를 포함한 FCM payload는 ${maxBytes}바이트 이하여야 합니다.")
        return objectMapper.writeValueAsString(mapOf("title" to command.title, "body" to command.body, "data" to data))
    }

    private fun validateTargets(command: CreateNotificationBatchCommand): Set<Long>? = when (command.targetType) {
        NotificationBatchTargetType.ALL -> {
            if (command.userIds != null) throw InvalidRequestFieldException("userIds", "전체 발송에서는 userIds를 보내지 않습니다.")
            null
        }
        NotificationBatchTargetType.USER_IDS -> {
            val ids = command.userIds
            if (ids.isNullOrEmpty() || ids.size > 1000 || ids.any { it <= 0 }) {
                throw InvalidRequestFieldException("userIds", "양수 회원 ID를 1개 이상 1000개 이하로 지정해야 합니다.")
            }
            ids.toSet()
        }
    }

    private fun validateData(data: Map<String, String>) {
        if (data.keys.any { it.isBlank() || it in setOf("from", "message_type", "gcm") || it.startsWith("google.") || it.startsWith("gcm.") }) {
            throw InvalidRequestFieldException("data", "빈 키나 FCM 예약 키를 사용할 수 없습니다.")
        }
        val url = data["url"]?.let { runCatching { URI(it) }.getOrNull() }
        if (url == null || url.scheme?.lowercase() !in setOf("http", "https") || url.host.isNullOrBlank()) {
            throw InvalidRequestFieldException("data.url", "이동할 http 또는 https URL이 필요합니다.")
        }
    }
}
