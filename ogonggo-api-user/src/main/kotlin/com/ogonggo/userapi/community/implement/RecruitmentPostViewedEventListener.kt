package com.ogonggo.userapi.community.implement

import com.ogonggo.core.community.implement.PostMetricManager
import com.ogonggo.userapi.community.business.RecruitmentPostViewedEvent
import com.ogonggo.userapi.config.UserAsyncConfiguration.Companion.METRIC_TASK_EXECUTOR
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/** 조회수 기록을 상세 조회 응답과 분리한다. */
@Component
class RecruitmentPostViewedEventListener(
    private val postMetricManager: PostMetricManager,
    private val clock: Clock,
) {

    @Async(METRIC_TASK_EXECUTOR)
    @EventListener
    @Transactional
    fun handle(event: RecruitmentPostViewedEvent) {
        try {
            postMetricManager.increaseViewCount(event.postId, LocalDateTime.now(clock))
        } catch (exception: Exception) {
            log.warn("모집글 조회 수 기록에 실패했습니다. postId={}", event.postId, exception)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RecruitmentPostViewedEventListener::class.java)
    }
}
