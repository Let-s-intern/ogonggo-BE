package com.ogonggo.userapi.notification.delivery.implement

import com.ogonggo.core.notification.delivery.implement.NotificationCleanupManager
import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

@Component
class NotificationCleanupScheduler(
    private val cleanupManager: NotificationCleanupManager,
    private val clock: Clock,
    private val executionObserver: SchedulerExecutionObserver,
) {
    @SchedulerLock(name = SCHEDULER_NAME, lockAtMostFor = "PT10M")
    fun cleanup() {
        executionObserver.observe(SCHEDULER_NAME) {
            val cutoff = LocalDateTime.now(clock).minusDays(RETENTION_DAYS)
            val startedAt = System.nanoTime()
            var deletedCount = 0
            var batchCount: Int
            do {
                batchCount = cleanupManager.deleteExpired(cutoff, BATCH_SIZE)
                deletedCount += batchCount
            } while (batchCount == BATCH_SIZE && System.nanoTime() - startedAt < RUN_BUDGET.toNanos())
            log.info(
                "오래된 알림 정리 완료. deletedCount={}, timeBudgetReached={}",
                deletedCount,
                batchCount == BATCH_SIZE && System.nanoTime() - startedAt >= RUN_BUDGET.toNanos(),
            )
        }
    }

    companion object {
        const val SCHEDULER_NAME = "notificationCleanup"
        private const val RETENTION_DAYS = 30L
        private const val BATCH_SIZE = 500
        private val RUN_BUDGET = Duration.ofSeconds(45)
        private val log = LoggerFactory.getLogger(NotificationCleanupScheduler::class.java)
    }
}
