package com.ogonggo.userapi.notification.intake.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.job.implement.JobBookmarkReminderNotificationKey
import com.ogonggo.core.job.implement.JobBookmarkReminderReader
import com.ogonggo.core.job.implement.JobBookmarkReminderScheduleManager
import com.ogonggo.core.job.implement.dto.JobBookmarkReminderCandidateDto
import com.ogonggo.core.job.implement.dto.JobBookmarkReminderScheduleDto
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.userapi.notification.channel.alimtalk.AlimTalkRecipientNumberPolicy
import com.ogonggo.userapi.notification.channel.alimtalk.ReminderAlimTalkTemplate
import com.ogonggo.userapi.notification.channel.alimtalk.dto.ReminderAlimTalkParametersDto
import com.ogonggo.userapi.notification.intake.business.dto.ReminderEnqueueResultDto
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

/** 스크랩 마감 리마인드의 대상 페이지를 알림으로 적재하고 일정 커서를 진행한다. */
@Service
internal class JobBookmarkReminderEnqueueService(
    private val scheduleManager: JobBookmarkReminderScheduleManager,
    private val reminderReader: JobBookmarkReminderReader,
    private val notificationAppender: NotificationAppender,
    private val objectMapper: ObjectMapper,
    transactionManager: PlatformTransactionManager,
) {
    private val pageTransaction = TransactionTemplate(transactionManager)

    /** 한 번에 평가할 일정 수. 실행 주기는 scheduled_jobs에서 별도로 관리한다. */
    fun enqueueDue(now: LocalDateTime): ReminderEnqueueResultDto {
        val dueSchedules = scheduleManager.readDueSchedules(now, WORK_BATCH_SIZE)
        return dueSchedules.fold(ReminderEnqueueResultDto()) { total, schedule ->
            // 페이지 실패가 앞서 커밋한 일정까지 롤백하지 않도록 일정별로 트랜잭션을 연다.
            val pageResult = checkNotNull(pageTransaction.execute { enqueueSchedulePage(schedule, now) })
            total + pageResult
        }
    }

    private fun enqueueSchedulePage(
        schedule: JobBookmarkReminderScheduleDto,
        now: LocalDateTime,
    ): ReminderEnqueueResultDto {
        val cursor = schedule.lastBookmarkId
        val candidates = readCandidatePage(schedule, cursor, now)
        val notifications = candidates.mapNotNull { candidate -> toNotification(schedule, candidate) }
        val queuedCount = notificationAppender.appendAll(notifications)

        // 알림 적재와 커서 이동을 한 트랜잭션으로 묶어 커서만 먼저 진행하는 일을 막는다.
        advanceSchedule(schedule, candidates, cursor)

        return ReminderEnqueueResultDto(
            workCount = 1,
            candidateCount = candidates.size,
            queuedCount = queuedCount,
            skippedCount = candidates.size - notifications.size,
        )
    }

    private fun readCandidatePage(
        schedule: JobBookmarkReminderScheduleDto,
        cursor: Long?,
        now: LocalDateTime,
    ): List<JobBookmarkReminderCandidateDto> = reminderReader.readEligibleCandidates(
        schedule = schedule.copy(lastBookmarkId = cursor),
        now = now,
        limit = CANDIDATE_PAGE_SIZE,
    )

    private fun toNotification(
        schedule: JobBookmarkReminderScheduleDto,
        candidate: JobBookmarkReminderCandidateDto,
    ): NotificationAppendDto? {
        val recipientNo = AlimTalkRecipientNumberPolicy.normalize(candidate.recipientNo) ?: return null
        val channel = NotificationChannel.KAKAO
        val template = ReminderAlimTalkTemplate.JOB_BOOKMARK_REMINDER

        return NotificationAppendDto(
            deduplicationKey = JobBookmarkReminderNotificationKey.forRecipient(
                jobId = candidate.jobId,
                userId = candidate.userId,
                channel = channel.name,
            ),
            channel = channel,
            templateCode = template.templateCode,
            recipientAddress = recipientNo,
            payloadJson = objectMapper.writeValueAsString(
                ReminderAlimTalkParametersDto(
                    name = candidate.recipientName,
                    postingTitle = candidate.postingTitle,
                ),
            ),
            scheduledAt = schedule.reminderAt,
            recipientUserId = candidate.userId,
        )
    }

    private fun advanceSchedule(
        schedule: JobBookmarkReminderScheduleDto,
        candidates: List<JobBookmarkReminderCandidateDto>,
        cursor: Long?,
    ) {
        scheduleManager.advanceSchedule(
            scheduleId = schedule.id,
            lastBookmarkId = candidates.lastOrNull()?.bookmarkId ?: cursor,
            isComplete = candidates.size < CANDIDATE_PAGE_SIZE,
        )
    }

    private companion object {
        const val WORK_BATCH_SIZE = 10
        const val CANDIDATE_PAGE_SIZE = 500
    }
}
