package com.ogonggo.userapi.notification.intake.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.job.implement.JobBookmarkReminderNotificationKey
import com.ogonggo.core.job.implement.JobBookmarkReminderReader
import com.ogonggo.core.job.implement.JobBookmarkReminderScheduleManager
import com.ogonggo.core.job.implement.dto.JobBookmarkReminderCandidateDto
import com.ogonggo.core.job.implement.dto.JobBookmarkReminderScheduleDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.time.LocalDateTime

class JobBookmarkReminderEnqueueServiceTest {

    @Test
    @DisplayName("due 평가에서 잘못된 전화번호는 건너뛰고 커서는 페이지 마지막 스크랩까지 진행한다")
    fun `유효한 번호만 스냅샷 큐에 넣고 페이지 진행을 보존한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 4, 9, 0)
        val schedule = JobBookmarkReminderScheduleDto(
            id = 55,
            jobId = 7,
            recruitmentEndAt = now.plusDays(1),
            reminderAt = now,
            lastBookmarkId = null,
        )
        val validCandidate = candidate(bookmarkId = 18, phoneNumber = "010-1234-5678")
        val invalidCandidate = candidate(bookmarkId = 19, phoneNumber = "011-1234-5678")
        val reader = Mockito.mock(JobBookmarkReminderReader::class.java)
        val appender = Mockito.mock(NotificationAppender::class.java)
        val scheduleManager = Mockito.mock(JobBookmarkReminderScheduleManager::class.java)
        Mockito.`when`(scheduleManager.readDueSchedules(now, 10)).thenReturn(listOf(schedule))
        Mockito.`when`(reader.readEligibleCandidates(schedule, now, 500))
            .thenReturn(listOf(validCandidate, invalidCandidate))
        val expectedNotification = NotificationAppendDto(
            deduplicationKey = "",
            channel = NotificationChannel.KAKAO,
            templateCode = "clip_remind",
            recipientAddress = "01012345678",
            payloadJson = ObjectMapper().writeValueAsString(
                mapOf("name" to "홍길동", "posting-title" to "백엔드 개발자"),
            ),
            scheduledAt = now,
            recipientUserId = 18,
        )
        var appendedNotifications: Collection<NotificationAppendDto>? = null
        Mockito.`when`(appender.appendAll(Mockito.anyCollection<NotificationAppendDto>())).thenAnswer { invocation ->
            appendedNotifications = invocation.getArgument(0)
            1
        }
        val transactionManager = DataSourceTransactionManager(
            DriverManagerDataSource(
                "jdbc:h2:mem:reminder-enqueue-service-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                "",
            ),
        )
        val enqueueService = JobBookmarkReminderEnqueueService(
            scheduleManager = scheduleManager,
            reminderReader = reader,
            notificationAppender = appender,
            objectMapper = ObjectMapper(),
            transactionManager = transactionManager,
        )

        // when
        val result = enqueueService.enqueueDue(now)

        // then
        assertEquals(1, result.workCount)
        assertEquals(1, result.queuedCount)
        assertEquals(2, result.candidateCount)
        assertEquals(1, result.skippedCount)
        Mockito.verify(scheduleManager).advanceSchedule(55, 19, true)
        val actualNotification = checkNotNull(appendedNotifications).single()
        assertEquals(expectedNotification.copy(deduplicationKey = actualNotification.deduplicationKey), actualNotification)
        assertTrue(actualNotification.deduplicationKey.startsWith(JobBookmarkReminderNotificationKey.jobPrefix(7)))
        assertTrue(actualNotification.deduplicationKey.endsWith(":user:18:KAKAO"))
    }

    private fun candidate(bookmarkId: Long, phoneNumber: String) = JobBookmarkReminderCandidateDto(
        bookmarkId = bookmarkId,
        jobId = 7,
        userId = bookmarkId,
        // 발송 예정 시각은 마감 시각에서 재계산하지 않고 저장된 일정(work.reminderAt)을 따라야 한다.
        recruitmentEndAt = LocalDateTime.of(2026, 10, 5, 9, 0, 0, 123_456_000),
        recipientNo = phoneNumber,
        recipientName = "홍길동",
        postingTitle = "백엔드 개발자",
    )
}
