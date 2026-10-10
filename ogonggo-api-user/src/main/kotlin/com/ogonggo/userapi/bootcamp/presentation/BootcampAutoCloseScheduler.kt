package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.userapi.bootcamp.business.BootcampAutoCloseService
import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/** 부트캠프의 모집 상태는 칼럼에 저장하므로 모집 종료 일시가 지나도 저절로 바뀌지 않는다. */
@Component
class BootcampAutoCloseScheduler(
    private val bootcampAutoCloseService: BootcampAutoCloseService,
    private val schedulerExecutionObserver: SchedulerExecutionObserver,
) {

    /** 실행 주기와 켜짐 여부는 `scheduled_jobs`가 정한다. `UserScheduledJobConfiguration` 참고. */
    @SchedulerLock(
        name = SCHEDULER_NAME,
        lockAtLeastFor = "\${ogonggo.bootcamp.auto-close.lock-at-least-for:PT55M}",
        lockAtMostFor = "\${ogonggo.bootcamp.auto-close.lock-at-most-for:PT2H}",
    )
    fun closeExpiredBootcamps() {
        val closedCount = schedulerExecutionObserver.observe(SCHEDULER_NAME) {
            bootcampAutoCloseService.closeExpired()
        }
        log.info("모집 기간 만료 부트캠프 자동 마감 완료. closedCount={}", closedCount)
    }

    companion object {
        const val SCHEDULER_NAME = "bootcampAutoClose"
        private val log = LoggerFactory.getLogger(BootcampAutoCloseScheduler::class.java)
    }
}
