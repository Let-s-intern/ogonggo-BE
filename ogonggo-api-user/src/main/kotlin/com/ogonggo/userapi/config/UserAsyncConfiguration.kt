package com.ogonggo.userapi.config

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

/**
 * 이 설정이 `Executor` 빈을 등록하므로 Spring Boot의 기본 `applicationTaskExecutor`는 만들어지지 않는다.
 * 현재는 MVC 비동기 반환형을 쓰는 엔드포인트가 없어 영향이 없지만,
 * `Callable`이나 `DeferredResult`를 반환하는 엔드포인트를 추가한다면 요청용 실행기를 명시적으로 등록해야 한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableConfigurationProperties(NotificationDeliveryProperties::class)
class UserAsyncConfiguration {

    /**
     * 조회 수 같은 지표 갱신 전용 실행기다.
     *
     * 스레드를 하나로 고정해 지표 기록이 DB 커넥션을 최대 1개만 점유하게 한다.
     * 지표 기록은 UPDATE 한 번이라 단일 스레드로도 충분하며, 늘리면 그만큼 요청 처리용 커넥션이 줄어든다.
     * 지표는 조회 응답의 정확성에 필요하지 않으므로 큐가 가득 차면 요청 스레드를 붙잡지 않고 버린다.
     */
    @Bean(name = [METRIC_TASK_EXECUTOR])
    fun metricTaskExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 1
        maxPoolSize = 1
        queueCapacity = 1_000
        setThreadNamePrefix("metric-")
        setRejectedExecutionHandler(ThreadPoolExecutor.DiscardPolicy())
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(5)
    }

    /**
     * 광고 문의 접수 확인 메일 전용 실행기다.
     *
     * 지표 실행기와 나눈다. 지표는 큐가 차면 버려도 되지만 확인 메일은 버리면 담당자가 접수 여부를 알 수 없고,
     * SMTP는 DB UPDATE보다 오래 걸려 단일 스레드를 오래 붙잡는다.
     * 대신 큐가 가득 차면 CallerRunsPolicy로 호출한 스레드가 직접 보내 유실 대신 지연을 택한다.
     */
    @Bean(name = [ADVERTISEMENT_TASK_EXECUTOR])
    fun advertisementTaskExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 1
        maxPoolSize = 2
        queueCapacity = 100
        setThreadNamePrefix("advertisement-")
        setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(5)
    }

    /**
     * 외부 알림 provider 호출을 요청·지표·문의 메일 작업과 분리한다.
     * 기본 동시성 4는 유지하고 작업마다 가상 스레드를 만든다. 포화 시 다음 tick에서 다시 읽는다.
     */
    @Bean(name = [NOTIFICATION_DELIVERY_TASK_EXECUTOR], destroyMethod = "close")
    @DependsOn(NOTIFICATION_RESULT_DB_TASK_EXECUTOR)
    fun notificationDeliveryTaskExecutor(
        properties: NotificationDeliveryProperties,
        meterRegistry: MeterRegistry,
    ): NotificationDeliveryTaskExecutor = NotificationDeliveryTaskExecutor(properties).apply {
        Gauge.builder("executor.active", this) { it.activeCount.toDouble() }
            .tag("name", NOTIFICATION_DELIVERY_TASK_EXECUTOR)
            .register(meterRegistry)
        Gauge.builder("executor.queued", this) { 0.0 }
            .tag("name", NOTIFICATION_DELIVERY_TASK_EXECUTOR)
            .register(meterRegistry)
    }

    /**
     * 발송 결과의 행별 트랜잭션을 실행하는 플랫폼 스레드 풀이다.
     * HTTP 응답을 기다리는 발송 작업과 JDBC 실행을 분리하고, 큐 용량을 동시 발송 상한으로 제한한다.
     */
    @Bean(name = [NOTIFICATION_RESULT_DB_TASK_EXECUTOR])
    fun notificationResultDbTaskExecutor(
        properties: NotificationDeliveryProperties,
        meterRegistry: MeterRegistry,
    ): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = properties.dbWorkers
        maxPoolSize = properties.dbWorkers
        queueCapacity = properties.dbQueueCapacity
        setThreadNamePrefix("notification-db-")
        setRejectedExecutionHandler(ThreadPoolExecutor.AbortPolicy())
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(15)
        val executor = this
        Gauge.builder("executor.active", executor) { it.activeCount.toDouble() }
            .tag("name", NOTIFICATION_RESULT_DB_TASK_EXECUTOR)
            .register(meterRegistry)
        Gauge.builder("executor.queued", executor) { it.threadPoolExecutor.queue.size.toDouble() }
            .tag("name", NOTIFICATION_RESULT_DB_TASK_EXECUTOR)
            .register(meterRegistry)
    }

    /**
     * 가입 AFTER_COMMIT 후 notification 적재 전용 풀이다. 단일 worker와 bounded queue로 DB 적재 부하를 제한한다.
     * CallerRuns 대신 AbortPolicy를 써서 포화 시 가입 요청 스레드에서 DB 작업을 실행하지 않고 listener가 유실을 기록한다.
     */
    @Bean(name = [NOTIFICATION_ENQUEUE_TASK_EXECUTOR])
    fun notificationEnqueueTaskExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 1
        maxPoolSize = 1
        queueCapacity = 1_000
        setThreadNamePrefix("notification-enqueue-")
        setRejectedExecutionHandler(ThreadPoolExecutor.AbortPolicy())
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(5)
    }

    /**
     * 기업회원 공고·부트캠프 등록 슬랙 알림 전용 실행기다.
     *
     * 등록은 드물어 스레드 하나로 충분하다. 공고는 DB에 남으므로 큐가 차면 알림을 버린다.
     * CallerRuns로 두면 커밋 직후 요청 스레드가 슬랙 응답을 기다리게 된다.
     */
    @Bean(name = [COMPANY_POSTING_TASK_EXECUTOR])
    fun companyPostingTaskExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 1
        maxPoolSize = 1
        queueCapacity = 100
        setThreadNamePrefix("company-posting-")
        setRejectedExecutionHandler(ThreadPoolExecutor.DiscardPolicy())
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(5)
    }

    companion object {
        const val METRIC_TASK_EXECUTOR = "metricTaskExecutor"
        const val ADVERTISEMENT_TASK_EXECUTOR = "advertisementTaskExecutor"
        const val NOTIFICATION_DELIVERY_TASK_EXECUTOR = "notificationDeliveryTaskExecutor"
        const val NOTIFICATION_RESULT_DB_TASK_EXECUTOR = "notificationResultDbTaskExecutor"
        const val NOTIFICATION_ENQUEUE_TASK_EXECUTOR = "notificationEnqueueTaskExecutor"
        const val COMPANY_POSTING_TASK_EXECUTOR = "companyPostingTaskExecutor"
    }
}
