package com.ogonggo.userapi.notification.intake.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.job.implement.JobBookmarkReminderNotificationKey
import com.ogonggo.core.job.implement.JobBookmarkReminderReader
import com.ogonggo.core.job.implement.dto.JobBookmarkReminderCandidateDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime

class JobBookmarkReminderEnqueueServiceTest {

    @Test
    @DisplayName("유효한 번호만 알림으로 적재하고 실행 중 커서를 페이지 마지막까지 진행한다")
    fun `유효한 번호만 알림으로 적재한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 4, 9, 0)
        val validCandidate = candidate(bookmarkId = 18, phoneNumber = "010-1234-5678")
        val invalidCandidate = candidate(bookmarkId = 19, phoneNumber = "011-1234-5678")
        val reader = Mockito.mock(JobBookmarkReminderReader::class.java)
        val appender = Mockito.mock(NotificationAppender::class.java)
        Mockito.`when`(reader.readEligibleCandidates(now, null, 500))
            .thenReturn(listOf(validCandidate, invalidCandidate))
        val expectedNotification = NotificationAppendDto(
            deduplicationKey = JobBookmarkReminderNotificationKey.forRecipient(
                jobId = 7,
                userId = 18,
                channel = NotificationChannel.KAKAO.name,
                reminderAt = validCandidate.reminderAt,
            ),
            channel = NotificationChannel.KAKAO,
            templateCode = "clip_remind",
            recipientAddress = "01012345678",
            payloadJson = ObjectMapper().writeValueAsString(
                mapOf("name" to "홍길동", "posting-title" to "백엔드 개발자"),
            ),
            scheduledAt = validCandidate.reminderAt,
            recipientUserId = 18,
        )
        var appendedNotifications: Collection<NotificationAppendDto>? = null
        Mockito.`when`(appender.appendAll(Mockito.anyCollection<NotificationAppendDto>())).thenAnswer { invocation ->
            appendedNotifications = invocation.getArgument(0)
            1
        }
        val enqueueService = JobBookmarkReminderEnqueueService(
            reminderReader = reader,
            notificationAppender = appender,
            objectMapper = ObjectMapper(),
        )

        // when
        val result = enqueueService.enqueueDue(now, afterBookmarkId = null)

        // then
        assertEquals(1, result.workCount)
        assertEquals(1, result.queuedCount)
        assertEquals(2, result.candidateCount)
        assertEquals(1, result.skippedCount)
        assertEquals(19L, result.lastBookmarkId)
        val actualNotification = checkNotNull(appendedNotifications).single()
        assertEquals(expectedNotification, actualNotification)
        Mockito.verify(reader).readEligibleCandidates(now, null, 500)
    }

    private fun candidate(bookmarkId: Long, phoneNumber: String) = JobBookmarkReminderCandidateDto(
        bookmarkId = bookmarkId,
        jobId = 7,
        userId = bookmarkId,
        // 발송 예정 시각은 마감 시각에서 재계산하지 않고 저장된 일정(work.reminderAt)을 따라야 한다.
        recruitmentEndAt = LocalDateTime.of(2026, 10, 5, 9, 0, 0, 123_456_000),
        reminderAt = LocalDateTime.of(2026, 10, 4, 9, 0, 0, 123_456_000),
        recipientNo = phoneNumber,
        recipientName = "홍길동",
        postingTitle = "백엔드 개발자",
    )
}
