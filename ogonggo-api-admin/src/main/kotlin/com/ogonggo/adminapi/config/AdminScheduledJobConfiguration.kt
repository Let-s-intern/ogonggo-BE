package com.ogonggo.adminapi.config

import com.ogonggo.adminapi.ingestion.work24.implement.Work24CollectionScheduler
import com.ogonggo.core.schedule.implement.dto.ScheduledJobDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 관리자 API의 스케줄 작업이다. cron은 DB(`scheduled_jobs`)에 행이 없을 때 쓰는 기본값이며,
 * 이미 있는 행은 배포해도 바뀌지 않는다. 작업 이름은 ShedLock 잠금 이름과 같다.
 *
 * 실행은 프록시 빈의 메서드를 부르므로 각 메서드의 `@SchedulerLock`이 그대로 걸린다.
 */
@Configuration(proxyBeanMethods = false)
class AdminScheduledJobConfiguration {

    @Bean
    fun work24DailyCollectionJob(scheduler: Work24CollectionScheduler) = ScheduledJobDefinition(
        name = Work24CollectionScheduler.SCHEDULER_NAME,
        defaultCron = "0 0 4 * * *",
        description = "고용24 채용정보·훈련과정을 채용공고·부트캠프로 등록 (매일 04:00)",
        action = scheduler::collect,
    )
}
