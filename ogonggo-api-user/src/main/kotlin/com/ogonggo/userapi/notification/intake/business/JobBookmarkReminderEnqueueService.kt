package com.ogonggo.userapi.notification.intake.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.job.implement.JobBookmarkReminderNotificationKey
import com.ogonggo.core.job.implement.JobBookmarkReminderReader
import com.ogonggo.core.job.implement.dto.JobBookmarkReminderCandidateDto
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.userapi.notification.channel.alimtalk.AlimTalkRecipientNumberPolicy
import com.ogonggo.userapi.notification.channel.alimtalk.ReminderAlimTalkTemplate
import com.ogonggo.userapi.notification.channel.alimtalk.dto.ReminderAlimTalkParametersDto
import com.ogonggo.userapi.notification.intake.business.dto.ReminderEnqueueResultDto
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/** 스크랩 마감 리마인드의 대상 페이지를 알림 대기열에 적재한다. */
@Service
internal class JobBookmarkReminderEnqueueService(
    private val reminderReader: JobBookmarkReminderReader,
    private val notificationAppender: NotificationAppender,
    private val objectMapper: ObjectMapper,
) {
    /** 한 번에 처리한 페이지와 다음 페이지의 일회성 커서를 반환한다. */
    fun enqueueDue(now: LocalDateTime, afterBookmarkId: Long?): ReminderEnqueueResultDto {
        val candidates = reminderReader.readEligibleCandidates(
            now = now,
            afterBookmarkId = afterBookmarkId,
            limit = CANDIDATE_PAGE_SIZE,
        )
        val notifications = candidates.mapNotNull(::toNotification)
        val queuedCount = notificationAppender.appendAll(notifications)

        return ReminderEnqueueResultDto(
            workCount = if (candidates.isEmpty()) 0 else 1,
            candidateCount = candidates.size,
            queuedCount = queuedCount,
            skippedCount = candidates.size - notifications.size,
            lastBookmarkId = candidates.lastOrNull()?.bookmarkId ?: afterBookmarkId,
        )
    }

    private fun toNotification(candidate: JobBookmarkReminderCandidateDto): NotificationAppendDto? {
        val recipientNo = AlimTalkRecipientNumberPolicy.normalize(candidate.recipientNo) ?: return null
        val channel = NotificationChannel.KAKAO
        val template = ReminderAlimTalkTemplate.JOB_BOOKMARK_REMINDER

        return NotificationAppendDto(
            deduplicationKey = JobBookmarkReminderNotificationKey.forRecipient(
                jobId = candidate.jobId,
                userId = candidate.userId,
                channel = channel.name,
                reminderAt = candidate.reminderAt,
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
            scheduledAt = candidate.reminderAt,
            recipientUserId = candidate.userId,
        )
    }

    private companion object {
        const val CANDIDATE_PAGE_SIZE = 500
    }
}
