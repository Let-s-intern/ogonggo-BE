package com.ogonggo.core.job.implement

import com.ogonggo.core.job.implement.event.JobRecruitmentDeadlineChangedEvent
import com.ogonggo.core.notification.delivery.implement.NotificationManager
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** 마감 변경 시 이전 마감 기준으로 아직 대기 중인 리마인드만 제거한다. */
@Component
internal class JobBookmarkReminderNotificationListener(
    private val notificationManager: NotificationManager,
) {

    @Transactional
    @EventListener
    fun on(event: JobRecruitmentDeadlineChangedEvent) {
        if (event.previousEndAt == null || event.previousEndAt == event.recruitmentEndAt) return

        val deletedNotifications = notificationManager.deletePendingByDeduplicationKeyPrefix(
            JobBookmarkReminderNotificationKey.jobPrefix(event.jobId),
        )
        if (deletedNotifications > 0) {
            log.info(
                "대기 중인 리마인드 알림을 제거했습니다. jobId={}, deleted={}",
                event.jobId,
                deletedNotifications,
            )
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(JobBookmarkReminderNotificationListener::class.java)
    }
}
