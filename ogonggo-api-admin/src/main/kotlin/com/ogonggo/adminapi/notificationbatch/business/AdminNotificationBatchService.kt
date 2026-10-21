package com.ogonggo.adminapi.notificationbatch.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.core.error.EntityNotFoundException
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
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    /** 적재와 요청 기록을 함께 커밋한다. provider 호출은 사용자 API의 기존 dispatcher가 맡는다. */
    @Transactional
    fun create(adminUserId: Long, command: CreateNotificationBatchCommand): NotificationBatchDto {
        val userIds = validateTargets(command)
        validateData(command.data)
        val payload = objectMapper.writeValueAsString(mapOf(
            "title" to command.title, "body" to command.body, "data" to command.data,
        ))
        // 문자열 길이와 별개로 한글·이모지와 data를 포함한 발송 payload 크기도 제한한다.
        val messageBytes = objectMapper.writeValueAsBytes(mapOf(
            "notification" to mapOf("title" to command.title, "body" to command.body), "data" to command.data,
        ))
        if (messageBytes.size > 4096) throw InvalidRequestFieldException("data", "제목·본문·data를 합친 FCM payload는 4096바이트 이하여야 합니다.")

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
                    recipient.tokens.isEmpty() -> { noTokenCount++; emptyList() }
                    else -> {
                        recipientCount++
                        recipient.tokens.map { token -> NotificationAppendDto(
                            deduplicationKey = "admin-batch:$batchId:token:${token.id}",
                            channel = NotificationChannel.FCM, templateCode = "admin_notification",
                            recipientUserId = recipient.userId, recipientAddress = token.token,
                            payloadJson = payload, scheduledAt = now, batchId = batchId, dispatchExpiresAt = expiresAt,
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

    fun get(batchId: UUID): NotificationBatchResultDto = batchReader.readResult(batchId.toString(), LocalDateTime.now(clock))

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
