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
import java.time.LocalDateTime
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
    ],
)
class MetricAsyncDispatchTest @Autowired constructor(
    private val eventPublisher: ApplicationEventPublisher,
    private val jobMetricManager: RecordingJobMetricManager,
    private val metricTaskExecutor: ThreadPoolTaskExecutor,
    private val transactionTemplate: TransactionTemplate,
) {

    @TestConfiguration
    class RecordingConfiguration {
        @Bean
        @Primary
        fun recordingJobMetricManager() = RecordingJobMetricManager()
    }

    @BeforeEach
    fun resetRecordedCalls() {
        jobMetricManager.reset()
    }

    @Test
    fun `조회 이벤트는 발행 스레드가 아니라 지표 실행기에서 처리된다`() {
        eventPublisher.publishEvent(JobViewedEvent(1L))

        val thread = jobMetricManager.awaitViewCount()

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

        val thread = jobMetricManager.awaitBookmarkCount()

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

        assertNull(jobMetricManager.awaitBookmarkCount(), "롤백된 북마크 변경이 지표에 반영됐습니다.")
    }

    @Test
    fun `지표 실행기는 DB 커넥션을 하나만 점유하도록 단일 스레드로 동작한다`() {
        assertEquals(1, metricTaskExecutor.corePoolSize)
        assertEquals(1, metricTaskExecutor.maxPoolSize)
    }
}

/** 스프링 컨텍스트를 공유하는 싱글턴이므로 기록을 테스트마다 비운다. */
class RecordingJobMetricManager : JobMetricManager {

    private val viewCountThreads = LinkedBlockingQueue<String>()
    private val bookmarkCountThreads = LinkedBlockingQueue<String>()

    override fun increaseViewCount(jobId: Long, now: LocalDateTime) {
        viewCountThreads.put(Thread.currentThread().name)
    }

    override fun syncBookmarkCount(jobId: Long) {
        bookmarkCountThreads.put(Thread.currentThread().name)
    }

    fun reset() {
        viewCountThreads.clear()
        bookmarkCountThreads.clear()
    }

    /** 지표 기록을 수행한 스레드 이름이며, 제한 시간 안에 실행되지 않으면 null이다. */
    fun awaitViewCount(): String? = viewCountThreads.poll(3, TimeUnit.SECONDS)

    fun awaitBookmarkCount(): String? = bookmarkCountThreads.poll(3, TimeUnit.SECONDS)
}
