package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.JobBookmarkReminderSchedule
import com.ogonggo.core.job.domain.JobBookmarkReminderScheduleStatus
import com.ogonggo.core.job.implement.dto.JobBookmarkReminderScheduleDto
import com.ogonggo.core.job.implement.event.JobRecruitmentDeadlineChangedEvent
import com.ogonggo.core.job.persistence.JobBookmarkReminderScheduleJpaRepository
import com.ogonggo.core.notification.intake.domain.NotificationTiming
import com.ogonggo.core.notification.delivery.implement.NotificationManager
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class JobBookmarkReminderScheduleManager internal constructor(
    private val scheduleRepository: JobBookmarkReminderScheduleJpaRepository,
    private val notificationManager: NotificationManager,
) {

    /**
     * 공고 마감 변경 이벤트를 공고 업무 트랜잭션 안에서 반영한다.
     * 이전 일정과 미발송 알림 취소, 새 D-1 일정 저장이 함께 커밋되므로 중간 상태가 남지 않는다.
     */
    @Transactional
    fun apply(event: JobRecruitmentDeadlineChangedEvent) {
        if (event.previousEndAt != null && event.previousEndAt != event.recruitmentEndAt) {
            scheduleRepository.findByJobIdAndRecruitmentEndAt(event.jobId, event.previousEndAt)
                ?.let { schedule ->
                    schedule.cancel()
                    // 아직 발송하지 않은 이전 일정의 알림 행만 제거한다. 이미 provider로 보낸 알림은 되돌릴 수 없다.
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
        }

        val recruitmentEndAt = event.recruitmentEndAt ?: return
        val reminderAt = NotificationTiming.MINUS_24_HOURS.calculateAt(recruitmentEndAt)
        // 새 D-1 시각이 이미 지났으면 뒤늦게 소급 발송하지 않도록 일정만 SKIPPED로 기록한다.
        val isFutureSchedule = reminderAt.isAfter(event.changedAt)
        val scheduleStatus = if (isFutureSchedule) {
            JobBookmarkReminderScheduleStatus.PENDING
        } else {
            JobBookmarkReminderScheduleStatus.SKIPPED
        }
        val existingSchedule = scheduleRepository.findByJobIdAndRecruitmentEndAt(event.jobId, recruitmentEndAt)
        if (existingSchedule == null) {
            scheduleRepository.save(
                JobBookmarkReminderSchedule(
                    jobId = event.jobId,
                    recruitmentEndAt = recruitmentEndAt,
                    reminderAt = reminderAt,
                    status = scheduleStatus,
                ),
            )
        } else {
            existingSchedule.applyChange(reminderAt, scheduleStatus)
        }
    }

    /** 매분 ShedLock으로 직렬화된 적재 스케줄러가 커서 뒤의 due 일정만 읽는다. */
    @Transactional
    fun readDueSchedules(now: LocalDateTime, limit: Int): List<JobBookmarkReminderScheduleDto> =
        scheduleRepository.findDue(
            status = JobBookmarkReminderScheduleStatus.PENDING,
            now = now,
            pageable = PageRequest.of(0, limit),
        ).map {
            JobBookmarkReminderScheduleDto(
                id = checkNotNull(it.id),
                jobId = it.jobId,
                recruitmentEndAt = it.recruitmentEndAt,
                reminderAt = it.reminderAt,
                lastBookmarkId = it.lastBookmarkId,
            )
        }

    @Transactional
    fun advanceSchedule(scheduleId: Long, lastBookmarkId: Long?, isComplete: Boolean) {
        // 호출자가 notification 적재와 같은 페이지 트랜잭션으로 감싸므로, 둘 중 하나만 반영되지 않는다.
        val schedule = scheduleRepository.findById(scheduleId).orElseThrow()
        schedule.advance(lastBookmarkId, isComplete)
    }

    private companion object {
        val log = LoggerFactory.getLogger(JobBookmarkReminderScheduleManager::class.java)
    }
}
