package com.ogonggo.core.job.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import com.ogonggo.core.review.implement.ContentRejectionManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
    @DisplayName("미래 D-1을 계산해 공고에 보관하고 마감 변경 시 다시 계산한다")
    fun `마감 변경에 따라 D-1 시각을 갱신한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 4, 9, 0)
        val firstEndAt = now.plusDays(5)
        val job = jobAppender.append(jobDto(firstEndAt), now)
        assertEquals(firstEndAt.minusHours(24), job.bookmarkReminderAt)

        // when
        val alreadyDueEndAt = now.plusHours(12)
        jobManager.update(job, updateDto(alreadyDueEndAt), now)

        // then
        assertNull(job.bookmarkReminderAt)
    }

    @Test
    @DisplayName("마감일이 바뀌면 기존 대기 알림을 제거한다")
    fun `마감 변경은 이전 대기 알림을 취소한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 4, 9, 0)
        val oldEndAt = now.plusDays(5)
        val job = jobAppender.append(jobDto(oldEndAt), now)
        val jobId = checkNotNull(job.id)
        val reminderAt = checkNotNull(job.bookmarkReminderAt)
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
        assertEquals(newEndAt.minusHours(24), job.bookmarkReminderAt)
    }

    private fun jobDto(recruitmentEndAt: LocalDateTime) = JobAppendDto(
        companyName = "오늘의공고",
        title = "백엔드 개발자",
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.NEWCOMER,
        recruitmentType = JobRecruitmentType.PERIOD,
        recruitmentEndAt = recruitmentEndAt,
    )

    private fun updateDto(recruitmentEndAt: LocalDateTime) = JobUpdateDto(
        companyName = "오늘의공고",
        title = "백엔드 개발자",
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.NEWCOMER,
        recruitmentType = JobRecruitmentType.PERIOD,
        recruitmentEndAt = recruitmentEndAt,
    )
}
