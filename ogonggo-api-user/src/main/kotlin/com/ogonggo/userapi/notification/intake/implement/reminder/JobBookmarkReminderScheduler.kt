package com.ogonggo.userapi.notification.intake.implement.reminder

import com.ogonggo.userapi.notification.intake.business.JobBookmarkReminderEnqueueService
import com.ogonggo.userapi.notification.intake.business.dto.ReminderEnqueueResultDto
import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Component
class JobBookmarkReminderScheduler internal constructor(
    private val enqueueService: JobBookmarkReminderEnqueueService,
    private val clock: Clock,
    private val executionObserver: SchedulerExecutionObserver,
) {
    /**
     * DB 등록 주기에 따라 due 대상을 반복 적재한다. ShedLock이 인스턴스 간 실행을 직렬화하고,
     * 진행 커서는 한 번의 실행 안에서만 유지하며, 재시작 시 notifications에 이미 적재된 대상을 건너뛴다.
     * NHN 호출은 delivery dispatcher에 맡긴다.
     */
    @SchedulerLock(name = SCHEDULER_NAME, lockAtLeastFor = "PT0S", lockAtMostFor = "PT10M")
    fun run() {
        // 한 번의 scheduler 실행 로그를 상관관계 ID로 묶는다. 페이지별 로그 대신 실행 요약만 남긴다.
        val runId = UUID.randomUUID().toString()
        val startedAt = System.nanoTime()
        executionObserver.observe(SCHEDULER_NAME) {
            val stats = JobBookmarkReminderRunStats()
            try {
                val stoppedWithoutDueSchedules = enqueueUntilStopped(startedAt, stats)
                logCompletion(runId, startedAt, stats, stoppedWithoutDueSchedules)
            } catch (exception: Exception) {
                logFailure(runId, startedAt, stats, exception)
                throw SafeReminderSchedulerException()
            }
        }
    }

    private fun enqueueUntilStopped(startedAt: Long, stats: JobBookmarkReminderRunStats): Boolean {
        var afterBookmarkId: Long? = null
        do {
            // 페이지 처리 도중 새 대상이 계속 생겨도 한 번의 실행이 무한히 이어지지 않게 시간 예산을 둔다.
            val result = enqueueService.enqueueDue(LocalDateTime.now(clock), afterBookmarkId)
            stats.include(result)
            if (result.workCount == 0) return true
            afterBookmarkId = result.lastBookmarkId
        } while (System.nanoTime() - startedAt < RUN_BUDGET.toNanos())

        return false
    }

    private fun logCompletion(
        runId: String,
        startedAt: Long,
        stats: JobBookmarkReminderRunStats,
        stoppedWithoutDueSchedules: Boolean,
    ) {
        val durationMs = (System.nanoTime() - startedAt) / 1_000_000
        val timeBudgetReached = !stoppedWithoutDueSchedules && durationMs >= RUN_BUDGET.toMillis()
        log.info(
            "리마인드 대상 적재 완료. runId={}, durationMs={}, workItems={}, candidates={}, queued={}, skipped={}, timeBudgetReached={}",
            runId,
            durationMs,
            stats.workItems,
            stats.candidates,
            stats.queued,
            stats.skipped,
            timeBudgetReached,
        )
    }

    private fun logFailure(runId: String, startedAt: Long, stats: JobBookmarkReminderRunStats, exception: Exception) {
        // enqueueDue가 반환한 배치만 집계한다. 실패한 호출 안에서 먼저 커밋된 페이지는 이 요약에 포함되지 않는다.
        log.error(
            "리마인드 대상 적재 실패. runId={}, durationMs={}, workItems={}, candidates={}, queued={}, skipped={}, errorType={}",
            runId,
            (System.nanoTime() - startedAt) / 1_000_000,
            stats.workItems,
            stats.candidates,
            stats.queued,
            stats.skipped,
            exception.javaClass.simpleName,
            exception,
        )
    }

    private class SafeReminderSchedulerException : RuntimeException("스크랩 마감 리마인드 적재 실패")

    companion object {
        const val SCHEDULER_NAME = "jobBookmarkAlimTalkReminder"
        private val RUN_BUDGET = Duration.ofSeconds(45)
        private val log = LoggerFactory.getLogger(JobBookmarkReminderScheduler::class.java)
    }
}

private data class JobBookmarkReminderRunStats(
    var workItems: Int = 0,
    var candidates: Int = 0,
    var queued: Int = 0,
    var skipped: Int = 0,
) {
    fun include(result: ReminderEnqueueResultDto) {
        workItems += result.workCount
        candidates += result.candidateCount
        queued += result.queuedCount
        skipped += result.skippedCount
    }
}
