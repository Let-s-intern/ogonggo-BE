package com.ogonggo.userapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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

    companion object {
        const val METRIC_TASK_EXECUTOR = "metricTaskExecutor"
    }
}
