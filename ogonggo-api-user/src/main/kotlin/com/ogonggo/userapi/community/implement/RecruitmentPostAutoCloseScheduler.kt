package com.ogonggo.userapi.community.implement

import com.ogonggo.core.community.implement.RecruitmentPostManager
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

@Component
class RecruitmentPostAutoCloseScheduler(
    private val recruitmentPostManager: RecruitmentPostManager,
    private val clock: Clock,
) {

    @Scheduled(fixedDelayString = "\${ogonggo.community.recruitment-post.auto-close.fixed-delay-ms:3600000}")
    @SchedulerLock(
        name = "communityRecruitmentPostAutoClose",
        lockAtLeastFor = "\${ogonggo.community.recruitment-post.auto-close.lock-at-least-for:PT55M}",
        lockAtMostFor = "\${ogonggo.community.recruitment-post.auto-close.lock-at-most-for:PT2H}",
    )
    fun closeExpiredRecruitmentPosts() {
        val now = LocalDateTime.now(clock)
        val closedCount = recruitmentPostManager.closeExpired(
            today = now.toLocalDate(),
            closedAt = now,
        )
        if (closedCount > 0) {
            log.info("기간 만료 모집글 자동 마감 완료. closedCount={}", closedCount)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RecruitmentPostAutoCloseScheduler::class.java)
    }
}
