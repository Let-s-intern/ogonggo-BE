package com.ogonggo.userapi.config

import com.ogonggo.core.schedule.implement.dto.ScheduledJobDefinition
import com.ogonggo.userapi.bootcamp.implement.BootcampAutoCloseScheduler
import com.ogonggo.userapi.community.implement.RecruitmentPostAutoCloseScheduler
import com.ogonggo.userapi.image.implement.ImageAssetCleanupScheduler
import com.ogonggo.userapi.job.implement.JobAutoCloseScheduler
import com.ogonggo.userapi.notification.delivery.implement.NotificationDispatcher
import com.ogonggo.userapi.notification.delivery.implement.NotificationCleanupScheduler
import com.ogonggo.userapi.user.implement.LetsCareerJobProfileSyncScheduler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 사용자 API의 스케줄 작업이다. cron은 DB(`scheduled_jobs`)에 행이 없을 때 쓰는 기본값이며,
 * 이미 있는 행은 배포해도 바뀌지 않는다. 작업 이름은 ShedLock 잠금 이름과 같다.
 *
 * 실행은 프록시 빈의 메서드를 부르므로 각 메서드의 `@SchedulerLock`이 그대로 걸린다.
 * NHN `clip_remind` 승인 전까지 스크랩 리마인드 작업은 임시 미등록 상태다.
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
    fun bootcampAutoCloseJob(scheduler: BootcampAutoCloseScheduler) = ScheduledJobDefinition(
        name = BootcampAutoCloseScheduler.SCHEDULER_NAME,
        defaultCron = "0 0 * * * *",
        description = "모집 종료 일시가 지난 부트캠프 자동 마감 (매시 정각)",
        action = scheduler::closeExpiredBootcamps,
    )

    @Bean
    fun jobAutoCloseJob(scheduler: JobAutoCloseScheduler) = ScheduledJobDefinition(
        name = JobAutoCloseScheduler.SCHEDULER_NAME,
        defaultCron = "0 0 * * * *",
        description = "모집 종료 일시가 지난 채용공고 자동 마감 (매시 정각)",
        action = scheduler::closeExpiredJobs,
    )

    @Bean
    fun imageAssetCleanupJob(scheduler: ImageAssetCleanupScheduler) = ScheduledJobDefinition(
        name = ImageAssetCleanupScheduler.SCHEDULER_NAME,
        defaultCron = "0 30 * * * *",
        description = "게시글에 쓰이지 않은 업로드 이미지 정리 (매시 30분)",
        action = scheduler::cleanup,
    )

    @Bean
    fun letsCareerJobProfileSyncJob(scheduler: LetsCareerJobProfileSyncScheduler) = ScheduledJobDefinition(
        name = LetsCareerJobProfileSyncScheduler.SCHEDULER_NAME,
        defaultCron = "*/30 * * * * *",
        description = "오공고에서 고친 학력·희망 조건을 렛츠커리어로 전송 (30초마다)",
        action = scheduler::sendPending,
    )

    /** 단일 DB 작업 잠금으로 발송 실행 전체를 직렬화한다. */
    @Bean
    fun jobBookmarkAlimTalkDeliveryJob(dispatcher: NotificationDispatcher) = ScheduledJobDefinition(
        name = NotificationDispatcher.SCHEDULER_NAME,
        defaultCron = "* * * * * *",
        description = "due notification 발송 (매초, 다중 인스턴스는 ShedLock으로 직렬화)",
        action = dispatcher::dispatch,
    )

    @Bean
    fun notificationCleanupJob(scheduler: NotificationCleanupScheduler) = ScheduledJobDefinition(
        name = NotificationCleanupScheduler.SCHEDULER_NAME,
        defaultCron = "0 30 3 * * *",
        description = "최종 상태로 바뀐 지 30일 지난 알림 정리 (매일 03:30)",
        action = scheduler::cleanup,
    )
}
