package com.ogonggo.userapi.community.implement

import com.ogonggo.core.community.implement.RecruitmentPostManager
import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

@Component
class RecruitmentPostAutoCloseScheduler(
    private val recruitmentPostManager: RecruitmentPostManager,
    private val clock: Clock,
    private val schedulerExecutionObserver: SchedulerExecutionObserver,
) {

    /** 실행 주기와 켜짐 여부는 `scheduled_jobs`가 정한다. `UserScheduledJobConfiguration` 참고. */
    @SchedulerLock(
        name = SCHEDULER_NAME,
        lockAtLeastFor = "\${ogonggo.community.recruitment-post.auto-close.lock-at-least-for:PT55M}",
        lockAtMostFor = "\${ogonggo.community.recruitment-post.auto-close.lock-at-most-for:PT2H}",
    )
    fun closeExpiredRecruitmentPosts() {
        val closedCount = schedulerExecutionObserver.observe(SCHEDULER_NAME) {
            val now = LocalDateTime.now(clock)
            recruitmentPostManager.closeExpired(
                today = now.toLocalDate(),
                closedAt = now,
            )
        }
        log.info("기간 만료 모집글 자동 마감 완료. closedCount={}", closedCount)
    }

    companion object {
        const val SCHEDULER_NAME = "communityRecruitmentPostAutoClose"
        private val log = LoggerFactory.getLogger(RecruitmentPostAutoCloseScheduler::class.java)
    }
}
