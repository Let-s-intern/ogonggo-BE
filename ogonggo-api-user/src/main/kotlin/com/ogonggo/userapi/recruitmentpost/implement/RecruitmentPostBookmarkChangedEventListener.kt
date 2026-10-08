package com.ogonggo.userapi.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostMetricManager
import com.ogonggo.userapi.config.UserAsyncConfiguration.Companion.METRIC_TASK_EXECUTOR
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostBookmarkChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Clock
import java.time.LocalDateTime

/**
 * 북마크 수를 북마크 트랜잭션 밖에서 맞춘다.
 * 북마크 트랜잭션 안에서 증감하면 같은 글에 북마크가 몰릴 때 북마크 행과 지표 행을 서로 기다릴 수 있고,
 * 카운터가 실제 수와 어긋나면 스스로 복구되지 않는다.
 *
 * 북마크가 롤백되면 지표도 바뀌면 안 되므로 커밋 이후에만 처리한다.
 * 다른 스레드에서 실행되어 발행자의 트랜잭션을 이어받을 수 없으므로 여기서 트랜잭션을 연다.
 * 갱신은 다시 세는 방식이라 한 번 놓쳐도 다음 북마크 변경에서 값이 복구되므로 실패는 로그만 남긴다.
 */
@Component
class RecruitmentPostBookmarkChangedEventListener(
    private val recruitmentPostMetricManager: RecruitmentPostMetricManager,
    private val clock: Clock,
) {

    @Async(METRIC_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: RecruitmentPostBookmarkChangedEvent) {
        try {
            recruitmentPostMetricManager.syncBookmarkCount(event.postId, LocalDateTime.now(clock))
        } catch (exception: Exception) {
            log.warn("모집글 북마크 수 갱신에 실패했습니다. postId={}", event.postId, exception)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RecruitmentPostBookmarkChangedEventListener::class.java)
    }
}
