package com.ogonggo.userapi.bootcamp.implement

import com.ogonggo.core.bootcamp.implement.BootcampMetricManager
import com.ogonggo.userapi.bootcamp.business.BootcampViewedEvent
import com.ogonggo.userapi.config.UserAsyncConfiguration.Companion.METRIC_TASK_EXECUTOR
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDateTime

/**
 * 조회 수는 조회 응답의 정확성에 필요하지 않으므로 응답을 막지 않고 지표 실행기에서 기록한다.
 *
 * 상세 조회에는 트랜잭션이 없으므로 `@TransactionalEventListener`를 쓰면 이벤트가 처리되지 않는다.
 * 다른 스레드에서 실행되어 발행자의 트랜잭션을 이어받을 수도 없으므로 여기서 트랜잭션을 연다.
 * 기록에 실패해도 이미 반환된 조회 응답에는 영향이 없어 로그만 남기고 삼킨다.
 * 트랜잭션은 예외를 잡는 범위 안쪽에서 열고 닫는다. 바깥에 두면 실패한 트랜잭션을 커밋하려다 예외가 다시 터진다.
 */
@Component
class BootcampViewedEventListener(
    private val bootcampMetricManager: BootcampMetricManager,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {

    @Async(METRIC_TASK_EXECUTOR)
    @EventListener
    fun handle(event: BootcampViewedEvent) {
        try {
            transactionTemplate.executeWithoutResult {
                bootcampMetricManager.increaseViewCount(event.bootcampId, LocalDateTime.now(clock))
            }
        } catch (exception: Exception) {
            log.warn("부트캠프 조회 수 기록에 실패했습니다. bootcampId={}", event.bootcampId, exception)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(BootcampViewedEventListener::class.java)
    }
}
