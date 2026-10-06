package com.ogonggo.userapi.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostMetricManager
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostViewedEvent
import com.ogonggo.userapi.config.UserAsyncConfiguration.Companion.METRIC_TASK_EXECUTOR
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDateTime

/**
 * 조회수 기록을 상세 조회 응답과 분리한다.
 *
 * 다른 스레드에서 실행되어 발행자의 트랜잭션을 이어받을 수 없으므로 여기서 트랜잭션을 연다.
 * 트랜잭션은 예외를 잡는 범위 안쪽에서 열고 닫는다. 바깥에 두면 실패한 트랜잭션을 커밋하려다 예외가 다시 터진다.
 */
@Component
class RecruitmentPostViewedEventListener(
    private val recruitmentPostMetricManager: RecruitmentPostMetricManager,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {

    @Async(METRIC_TASK_EXECUTOR)
    @EventListener
    fun handle(event: RecruitmentPostViewedEvent) {
        try {
            transactionTemplate.executeWithoutResult {
                recruitmentPostMetricManager.increaseViewCount(event.postId, LocalDateTime.now(clock))
            }
        } catch (exception: Exception) {
            log.warn("모집글 조회 수 기록에 실패했습니다. postId={}", event.postId, exception)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RecruitmentPostViewedEventListener::class.java)
    }
}
