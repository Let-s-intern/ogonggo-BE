package com.ogonggo.userapi.job.presentation

import com.ogonggo.userapi.job.business.JobAutoCloseService
import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/** 채용공고의 모집 상태는 등록·수정·마감할 때만 정하므로 모집 종료 일시가 지난 것은 이 작업이 반영한다. */
@Component
class JobAutoCloseScheduler(
    private val jobAutoCloseService: JobAutoCloseService,
    private val schedulerExecutionObserver: SchedulerExecutionObserver,
) {

    /** 실행 주기와 켜짐 여부는 `scheduled_jobs`가 정한다. `UserScheduledJobConfiguration` 참고. */
    @SchedulerLock(
        name = SCHEDULER_NAME,
        lockAtLeastFor = "\${ogonggo.job.auto-close.lock-at-least-for:PT55M}",
        lockAtMostFor = "\${ogonggo.job.auto-close.lock-at-most-for:PT2H}",
    )
    fun closeExpiredJobs() {
        val closedCount = schedulerExecutionObserver.observe(SCHEDULER_NAME) {
            jobAutoCloseService.closeExpired()
        }
        log.info("모집 기간 만료 채용공고 자동 마감 완료. closedCount={}", closedCount)
    }

    companion object {
        const val SCHEDULER_NAME = "jobAutoClose"
        private val log = LoggerFactory.getLogger(JobAutoCloseScheduler::class.java)
    }
}
