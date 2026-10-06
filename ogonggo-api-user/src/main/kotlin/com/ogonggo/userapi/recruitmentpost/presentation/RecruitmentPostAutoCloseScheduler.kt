package com.ogonggo.userapi.recruitmentpost.presentation

import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostAutoCloseService
import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RecruitmentPostAutoCloseScheduler(
    private val recruitmentPostAutoCloseService: RecruitmentPostAutoCloseService,
    private val schedulerExecutionObserver: SchedulerExecutionObserver,
) {

    /** 실행 주기와 켜짐 여부는 `scheduled_jobs`가 정한다. `UserScheduledJobConfiguration` 참고. */
    @SchedulerLock(
        name = SCHEDULER_NAME,
        lockAtLeastFor = "\${ogonggo.recruitment-post.auto-close.lock-at-least-for:PT55M}",
        lockAtMostFor = "\${ogonggo.recruitment-post.auto-close.lock-at-most-for:PT2H}",
    )
    fun closeExpiredRecruitmentPosts() {
        val closedCount = schedulerExecutionObserver.observe(SCHEDULER_NAME) {
            recruitmentPostAutoCloseService.closeExpired()
        }
        log.info("기간 만료 모집글 자동 마감 완료. closedCount={}", closedCount)
    }

    companion object {
        const val SCHEDULER_NAME = "recruitmentPostAutoClose"
        private val log = LoggerFactory.getLogger(RecruitmentPostAutoCloseScheduler::class.java)
    }
}
