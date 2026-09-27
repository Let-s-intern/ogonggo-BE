package com.ogonggo.userapi.config

import com.ogonggo.core.schedule.implement.dto.ScheduledJobDefinition
import com.ogonggo.userapi.community.implement.RecruitmentPostAutoCloseScheduler
import com.ogonggo.userapi.image.implement.ImageAssetCleanupScheduler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 사용자 API의 스케줄 작업이다. cron은 DB(`scheduled_jobs`)에 행이 없을 때 쓰는 기본값이며,
 * 이미 있는 행은 배포해도 바뀌지 않는다. 작업 이름은 ShedLock 잠금 이름과 같다.
 *
 * 실행은 프록시 빈의 메서드를 부르므로 각 메서드의 `@SchedulerLock`이 그대로 걸린다.
 */
@Configuration(proxyBeanMethods = false)
class UserScheduledJobConfiguration {

    @Bean
    fun recruitmentPostAutoCloseJob(scheduler: RecruitmentPostAutoCloseScheduler) = ScheduledJobDefinition(
        name = RecruitmentPostAutoCloseScheduler.SCHEDULER_NAME,
        defaultCron = "0 0 * * * *",
        description = "모집 기간이 끝난 사이드·스터디 모집글 자동 마감 (매시 정각)",
        action = scheduler::closeExpiredRecruitmentPosts,
    )

    @Bean
    fun imageAssetCleanupJob(scheduler: ImageAssetCleanupScheduler) = ScheduledJobDefinition(
        name = ImageAssetCleanupScheduler.SCHEDULER_NAME,
        defaultCron = "0 30 * * * *",
        description = "게시글에 쓰이지 않은 업로드 이미지 정리 (매시 30분)",
        action = scheduler::cleanup,
    )
}
