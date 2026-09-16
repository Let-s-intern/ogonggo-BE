package com.ogonggo.userapi.community.implement

import com.ogonggo.core.community.implement.RecruitmentPostManager
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class RecruitmentPostAutoCloseScheduler(
    private val recruitmentPostManager: RecruitmentPostManager,
    private val clock: Clock,
) {

    @Scheduled(fixedDelayString = "\${ogonggo.community.recruitment-post.auto-close.fixed-delay-ms:3600000}")
    fun closeExpiredRecruitmentPosts() {
        val closedCount = recruitmentPostManager.closeExpired(
            today = LocalDate.now(clock),
            closedAt = LocalDateTime.now(clock),
        )
        if (closedCount > 0) {
            log.info("기간 만료 모집글 자동 마감 완료. closedCount={}", closedCount)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RecruitmentPostAutoCloseScheduler::class.java)
    }
}
