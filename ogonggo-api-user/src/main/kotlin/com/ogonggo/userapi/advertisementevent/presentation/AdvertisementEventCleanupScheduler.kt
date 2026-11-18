package com.ogonggo.userapi.advertisementevent.presentation

import com.ogonggo.userapi.advertisementevent.business.AdvertisementEventCleanupService
import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

@Component
class AdvertisementEventCleanupScheduler(
    private val cleanupService: AdvertisementEventCleanupService,
    private val clock: Clock,
    private val executionObserver: SchedulerExecutionObserver,
) {

    @SchedulerLock(name = SCHEDULER_NAME, lockAtMostFor = "PT10M")
    fun cleanup() {
        executionObserver.observe(SCHEDULER_NAME) {
            val cutoff = LocalDate.now(clock.withZone(SEOUL)).minusYears(1)
            val startedAt = System.nanoTime()
            var deletedCount = 0
            var batchCount: Int
            do {
                batchCount = cleanupService.deleteExpired(cutoff, BATCH_SIZE)
                deletedCount += batchCount
            } while (batchCount > 0 && System.nanoTime() - startedAt < RUN_BUDGET.toNanos())

            log.info(
                "오래된 광고 이벤트 정리 완료. deletedCount={}, timeBudgetReached={}",
                deletedCount,
                batchCount > 0 && System.nanoTime() - startedAt >= RUN_BUDGET.toNanos(),
            )
        }
    }

    companion object {
        const val SCHEDULER_NAME = "advertisementEventCleanup"
        private const val BATCH_SIZE = 500
        private val RUN_BUDGET = Duration.ofSeconds(45)
        private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        private val log = LoggerFactory.getLogger(AdvertisementEventCleanupScheduler::class.java)
    }
}
