package com.ogonggo.core.notificationbatch.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.notificationbatch.error.NotificationBatchErrorCode
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchResultDto
import com.ogonggo.core.notificationbatch.persistence.NotificationBatchJpaRepository
import com.ogonggo.core.notificationbatch.persistence.NotificationBatchQueryRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NotificationBatchReader internal constructor(
    private val repository: NotificationBatchJpaRepository,
    private val queries: NotificationBatchQueryRepository,
) {
    fun readResult(batchId: String, now: LocalDateTime): NotificationBatchResultDto {
        val batch = repository.findById(batchId).orElse(null)
            ?: throw EntityNotFoundException(NotificationBatchErrorCode.NOTIFICATION_BATCH_NOT_FOUND)
        // 원본의 30일 정리가 시작된 뒤 불완전한 집계를 성공 결과처럼 내보내지 않는다.
        if (!now.isBefore(batch.createdAt.plusDays(30))) {
            throw EntityNotFoundException(NotificationBatchErrorCode.NOTIFICATION_BATCH_NOT_FOUND)
        }
        return NotificationBatchResultDto(batch.toDto(), queries.countResults(batchId), now)
    }
}
