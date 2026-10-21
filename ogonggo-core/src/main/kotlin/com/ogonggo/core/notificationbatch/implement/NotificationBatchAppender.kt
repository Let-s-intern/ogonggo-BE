package com.ogonggo.core.notificationbatch.implement

import com.ogonggo.core.notificationbatch.domain.NotificationBatch
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchAppendDto
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchDto
import com.ogonggo.core.notificationbatch.persistence.NotificationBatchJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class NotificationBatchAppender internal constructor(private val repository: NotificationBatchJpaRepository) {
    @Transactional
    fun append(dto: NotificationBatchAppendDto): NotificationBatchDto = repository.saveAndFlush(
        NotificationBatch(
            id = dto.batchId, requestedByUserId = dto.requestedByUserId, targetType = dto.targetType,
            requestedUserIdsJson = dto.requestedUserIdsJson, title = dto.title, body = dto.body, dataJson = dto.dataJson,
            targetUserCount = dto.targetUserCount, recipientUserCount = dto.recipientUserCount,
            enqueuedCount = dto.enqueuedCount, skippedNoTokenUserCount = dto.skippedNoTokenUserCount,
            skippedInactiveUserCount = dto.skippedInactiveUserCount, dispatchExpiresAt = dto.dispatchExpiresAt,
        ),
    ).toDto()
}

internal fun NotificationBatch.toDto() = NotificationBatchDto(
    batchId = id, requestedByUserId = requestedByUserId, targetType = targetType, title = title, body = body,
    dataJson = dataJson, targetUserCount = targetUserCount, recipientUserCount = recipientUserCount,
    enqueuedCount = enqueuedCount, skippedNoTokenUserCount = skippedNoTokenUserCount,
    skippedInactiveUserCount = skippedInactiveUserCount, dispatchExpiresAt = dispatchExpiresAt, createdAt = createdAt,
)
