package com.ogonggo.userapi.job.implement

import com.ogonggo.core.job.implement.JobMetricManager
import com.ogonggo.userapi.config.UserAsyncConfiguration.Companion.METRIC_TASK_EXECUTOR
import com.ogonggo.userapi.job.business.JobBookmarkChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 북마크 수를 북마크 트랜잭션 밖에서 맞춘다.
 * 공고 행에 잠금을 건 채로 세지 않으므로 인기 공고의 잠금 유지 시간이 짧아진다.
 *
 * 북마크가 롤백되면 지표도 바뀌면 안 되므로 커밋 이후에만 처리한다.
 * 다른 스레드에서 실행되어 발행자의 트랜잭션을 이어받을 수 없으므로 여기서 트랜잭션을 연다.
 * 갱신은 다시 세는 방식이라 한 번 놓쳐도 다음 북마크 변경에서 값이 복구되므로 실패는 로그만 남긴다.
 */
@Component
class JobBookmarkChangedEventListener(
    private val jobMetricManager: JobMetricManager,
) {

    @Async(METRIC_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: JobBookmarkChangedEvent) {
        try {
            jobMetricManager.syncBookmarkCount(event.jobId)
        } catch (exception: Exception) {
            log.warn("채용공고 북마크 수 갱신에 실패했습니다. jobId={}", event.jobId, exception)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(JobBookmarkChangedEventListener::class.java)
    }
}
