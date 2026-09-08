package com.ogonggo.userapi.config

import com.ogonggo.core.job.implement.JobMetricManager
import com.ogonggo.userapi.job.business.JobBookmarkChangedEvent
import com.ogonggo.userapi.job.business.JobViewedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.support.TransactionTemplate
import org.mockito.Mockito
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * 지표 이벤트 배선은 어긋나도 예외 없이 조용히 실패한다.
 * `@Async`가 걸리지 않으면 발행 스레드에서 실행되고, 트랜잭션 없이 `AFTER_COMMIT`을 쓰면 아예 실행되지 않는다.
 * 두 회귀를 모두 잡기 위해 실행 스레드와 실행 여부를 직접 확인한다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:ogonggo-user-metric-async;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ogonggo.auth.jwt.secret=b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA==",
        "ogonggo.letscareer.base-url=http://localhost:8090",
        "ogonggo.letscareer.internal-api-key=test-internal-api-key",
        // spring.mail.host가 있어야 Spring Boot가 JavaMailSender를 만든다. 실제로 발송하지는 않는다.
        "spring.mail.host=localhost",
    ],
)
class MetricAsyncDispatchTest @Autowired constructor(
    private val eventPublisher: ApplicationEventPublisher,
    private val metricCalls: MetricCallRecorder,
    private val metricTaskExecutor: ThreadPoolTaskExecutor,
    private val transactionTemplate: TransactionTemplate,
) {

    @TestConfiguration
    class RecordingConfiguration {
        @Bean
        fun metricCallRecorder() = MetricCallRecorder()

        /**
         * 지표 갱신이 어느 스레드에서 실행됐는지만 확인하면 되므로 실제 구현 대신 기록기를 끼운다.
         * 메서드마다 인자 매처를 쓰지 않도록 모든 호출을 한 Answer로 받는다.
         */
        @Bean
        @Primary
        fun recordingJobMetricManager(recorder: MetricCallRecorder): JobMetricManager =
            Mockito.mock(
                JobMetricManager::class.java,
                Mockito.withSettings().defaultAnswer { invocation ->
                    recorder.record(invocation.method.name, Thread.currentThread().name)
                    null
                },
            )
    }

    @BeforeEach
    fun resetRecordedCalls() {
        metricCalls.reset()
    }

    @Test
    fun `조회 이벤트는 발행 스레드가 아니라 지표 실행기에서 처리된다`() {
        eventPublisher.publishEvent(JobViewedEvent(1L))

        val thread = metricCalls.await("increaseViewCount")

        assertTrue(
            thread?.startsWith("metric-") == true,
            "조회 수 기록이 지표 실행기가 아닌 $thread 에서 실행됐습니다.",
        )
    }

    @Test
    fun `북마크 변경 이벤트는 커밋 이후 지표 실행기에서 처리된다`() {
        transactionTemplate.executeWithoutResult {
            eventPublisher.publishEvent(JobBookmarkChangedEvent(1L))
        }

        val thread = metricCalls.await("syncBookmarkCount")

        assertTrue(
            thread?.startsWith("metric-") == true,
            "북마크 수 갱신이 지표 실행기가 아닌 $thread 에서 실행됐습니다.",
        )
    }

    @Test
    fun `북마크 변경 이벤트는 롤백되면 처리되지 않는다`() {
        transactionTemplate.executeWithoutResult { status ->
            eventPublisher.publishEvent(JobBookmarkChangedEvent(2L))
            status.setRollbackOnly()
        }

        assertNull(metricCalls.await("syncBookmarkCount"), "롤백된 북마크 변경이 지표에 반영됐습니다.")
    }

    @Test
    fun `지표 실행기는 DB 커넥션을 하나만 점유하도록 단일 스레드로 동작한다`() {
        assertEquals(1, metricTaskExecutor.corePoolSize)
        assertEquals(1, metricTaskExecutor.maxPoolSize)
    }
}

/**
 * 지표 갱신을 수행한 스레드 이름을 메서드별로 모은다.
 * 스프링 컨텍스트를 공유하는 싱글턴이므로 기록을 테스트마다 비운다.
 */
class MetricCallRecorder {

    private val threadsByMethod = ConcurrentHashMap<String, LinkedBlockingQueue<String>>()

    fun record(method: String, thread: String) {
        threadsByMethod.computeIfAbsent(method) { LinkedBlockingQueue() }.put(thread)
    }

    fun reset() = threadsByMethod.clear()

    /** 해당 지표 갱신을 수행한 스레드 이름이며, 제한 시간 안에 실행되지 않으면 null이다. */
    fun await(method: String): String? =
        threadsByMethod.computeIfAbsent(method) { LinkedBlockingQueue() }.poll(3, TimeUnit.SECONDS)
}
