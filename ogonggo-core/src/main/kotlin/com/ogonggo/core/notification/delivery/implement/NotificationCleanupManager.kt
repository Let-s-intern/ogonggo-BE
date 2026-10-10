package com.ogonggo.core.notification.delivery.implement

import com.ogonggo.core.notification.domain.NotificationStatus
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class NotificationCleanupManager internal constructor(
    private val notificationRepository: NotificationJpaRepository,
) {
    /** 한 배치만 삭제하고 커밋한다. 호출자가 다음 배치를 반복할 수 있다. */
    @Transactional
    fun deleteExpired(cutoff: LocalDateTime, batchSize: Int): Int {
        require(batchSize > 0)
        val ids = notificationRepository.findExpiredTerminalIds(
            statuses = TERMINAL_STATUSES,
            cutoff = cutoff,
            pageable = PageRequest.of(0, batchSize),
        )
        if (ids.isNotEmpty()) notificationRepository.deleteAllByIdInBatch(ids)
        return ids.size
    }

    private companion object {
        val TERMINAL_STATUSES = listOf(
            NotificationStatus.SENT,
            NotificationStatus.FAILED,
            NotificationStatus.UNKNOWN,
        )
    }
}
