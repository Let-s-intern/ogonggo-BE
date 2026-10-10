package com.ogonggo.core.job.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import com.ogonggo.core.contentreview.implement.ContentRejectionManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    JobAppender::class,
    JobManager::class,
    JobBookmarkReminderNotificationListener::class,
    NotificationAppender::class,
    NotificationManager::class,
    ContentRejectionManager::class,
)
internal class JobBookmarkReminderNotificationPersistenceTest @Autowired constructor(
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
    private val notificationAppender: NotificationAppender,
    private val notificationRepository: NotificationJpaRepository,
) {

    @Test
    @DisplayName("마감일이 바뀌면 기존 대기 알림을 제거한다")
    fun `마감 변경은 이전 대기 알림을 취소한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 4, 9, 0)
        val oldEndAt = now.plusDays(5)
        val job = jobAppender.append(jobDto(oldEndAt), now)
        val jobId = checkNotNull(job.id)
        val reminderAt = oldEndAt.minusHours(24)
        val key = JobBookmarkReminderNotificationKey.forRecipient(
            jobId = jobId,
            userId = 17,
            channel = NotificationChannel.KAKAO.name,
            reminderAt = reminderAt,
        )
        notificationAppender.append(
            NotificationAppendDto(
                deduplicationKey = key,
                channel = NotificationChannel.KAKAO,
                templateCode = "clip_remind",
                recipientAddress = "01012345678",
                payloadJson = "{}",
                scheduledAt = reminderAt,
                recipientUserId = 17,
            ),
        )

        // when
        val newEndAt = now.plusDays(7)
        jobManager.update(job, updateDto(newEndAt), now.plusHours(1))

        // then
        assertEquals(0, notificationRepository.findAllByDeduplicationKeyIn(listOf(key)).size)
    }

    private fun jobDto(recruitmentEndAt: LocalDateTime) = JobAppendDto(
        companyName = "오늘의공고",
        title = "백엔드 개발자",
        employmentType = JobEmploymentType.FULL_TIME,
        experienceType = JobExperienceType.NEWCOMER,
        recruitmentType = JobRecruitmentType.PERIOD,
        recruitmentEndAt = recruitmentEndAt,
    )

    private fun updateDto(recruitmentEndAt: LocalDateTime) = JobUpdateDto(
        companyName = "오늘의공고",
        title = "백엔드 개발자",
        employmentType = JobEmploymentType.FULL_TIME,
        experienceType = JobExperienceType.NEWCOMER,
        recruitmentType = JobRecruitmentType.PERIOD,
        recruitmentEndAt = recruitmentEndAt,
    )
}
