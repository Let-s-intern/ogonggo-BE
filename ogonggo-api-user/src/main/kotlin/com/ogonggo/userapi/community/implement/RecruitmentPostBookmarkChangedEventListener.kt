package com.ogonggo.userapi.community.implement

import com.ogonggo.core.community.implement.PostMetricManager
import com.ogonggo.userapi.community.business.RecruitmentPostBookmarkChangedEvent
import com.ogonggo.userapi.config.UserAsyncConfiguration.Companion.METRIC_TASK_EXECUTOR
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Clock
import java.time.LocalDateTime

/** 북마크 트랜잭션 커밋 이후 모집글의 활성 북마크 수를 지표에 반영한다. */
@Component
class RecruitmentPostBookmarkChangedEventListener(
    private val postMetricManager: PostMetricManager,
    private val clock: Clock,
) {

    @Async(METRIC_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: RecruitmentPostBookmarkChangedEvent) {
        try {
            postMetricManager.syncBookmarkCount(event.postId, LocalDateTime.now(clock))
        } catch (exception: Exception) {
            log.warn("모집글 북마크 수 갱신에 실패했습니다. postId={}", event.postId, exception)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RecruitmentPostBookmarkChangedEventListener::class.java)
    }
}
