package com.ogonggo.core.job.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.job.domain.JobBookmarkReminderScheduleStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.job.persistence.JobBookmarkReminderScheduleJpaRepository
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import com.ogonggo.core.review.implement.ContentRejectionManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.implement.dto.JobAppendDto

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    JobAppender::class,
    JobManager::class,
    JobBookmarkReminderScheduleListener::class,
    JobBookmarkReminderScheduleManager::class,
    NotificationAppender::class,
    NotificationManager::class,
    ContentRejectionManager::class,
)
internal class JobBookmarkReminderSchedulePersistenceTest @Autowired constructor(
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
    private val scheduleRepository: JobBookmarkReminderScheduleJpaRepository,
    private val notificationAppender: NotificationAppender,
    private val notificationRepository: NotificationJpaRepository,
) {

    @Test
    @DisplayName("새 공고의 D-1이 미래면 마감 변경 이벤트로 알림 일정을 기록한다")
    fun `미래 마감일의 알림 일정을 저장한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 4, 9, 0)
        val recruitmentEndAt = now.plusDays(5)

        // when
        val job = jobAppender.append(
            JobAppendDto(
                companyName = "오늘의공고",
                title = "백엔드 개발자",
                employmentType = EmploymentType.FULL_TIME,
                experienceType = ExperienceType.NEWCOMER,
                recruitmentType = JobRecruitmentType.PERIOD,
                recruitmentEndAt = recruitmentEndAt,
            ),
            now,
        )

        // then
        val schedule = scheduleRepository.findByJobIdAndRecruitmentEndAt(checkNotNull(job.id), recruitmentEndAt)
        assertNotNull(schedule)
        assertEquals(JobBookmarkReminderScheduleStatus.PENDING, schedule?.status)
        assertEquals(recruitmentEndAt.minusHours(24), schedule?.reminderAt)
    }

    @Test
    @DisplayName("마감일 변경 시 이전 미처리 일정은 취소하고 이미 지난 새 D-1은 소급 등록하지 않는다")
    fun `이미 지난 새 예정 시각은 취소된 일정만 남긴다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 4, 9, 0)
        val oldEndAt = now.plusDays(5)
        val job = jobAppender.append(
            JobAppendDto(
                companyName = "오늘의공고",
                title = "백엔드 개발자",
                employmentType = EmploymentType.FULL_TIME,
                experienceType = ExperienceType.NEWCOMER,
                recruitmentType = JobRecruitmentType.PERIOD,
                recruitmentEndAt = oldEndAt,
            ),
            now,
        )
        val jobId = checkNotNull(job.id)
        val futureEndAt = now.plusDays(3)
        jobManager.update(job, updateDto(futureEndAt), now)
        assertEquals(
            JobBookmarkReminderScheduleStatus.PENDING,
            scheduleRepository.findByJobIdAndRecruitmentEndAt(jobId, futureEndAt)?.status,
        )
        val alreadyPassedEndAt = now.plusHours(12)

        // when
        jobManager.update(job, updateDto(alreadyPassedEndAt), now)

        // then
        assertEquals(
            JobBookmarkReminderScheduleStatus.CANCELLED,
            scheduleRepository.findByJobIdAndRecruitmentEndAt(jobId, oldEndAt)?.status,
        )
        assertEquals(
            JobBookmarkReminderScheduleStatus.CANCELLED,
            scheduleRepository.findByJobIdAndRecruitmentEndAt(jobId, futureEndAt)?.status,
        )
        assertEquals(
            JobBookmarkReminderScheduleStatus.SKIPPED,
            scheduleRepository.findByJobIdAndRecruitmentEndAt(jobId, alreadyPassedEndAt)?.status,
        )
    }

    @Test
    @DisplayName("마감일이 되돌아오면 일정 행을 재사용하고 이전 대기 알림은 제거한다")
    fun `A에서 B로 바뀐 뒤 다시 A로 돌아오면 한 일정 행에 새 변경 ID를 저장한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 4, 9, 0)
        val endAtA = now.plusDays(5)
        val endAtB = now.plusDays(6)
        val job = jobAppender.append(
            JobAppendDto(
                companyName = "오늘의공고",
                title = "백엔드 개발자",
                employmentType = EmploymentType.FULL_TIME,
                experienceType = ExperienceType.NEWCOMER,
                recruitmentType = JobRecruitmentType.PERIOD,
                recruitmentEndAt = endAtA,
            ),
            now,
        )
        val jobId = checkNotNull(job.id)
        val firstSchedule = scheduleRepository.findAll().single { it.jobId == jobId && it.recruitmentEndAt == endAtA }
        val oldKey = JobBookmarkReminderNotificationKey.forRecipient(
            jobId = jobId,
            userId = 17,
            channel = NotificationChannel.KAKAO.name,
        )
        notificationAppender.append(
            NotificationAppendDto(
                deduplicationKey = oldKey,
                channel = NotificationChannel.KAKAO,
                templateCode = "clip_remind",
                recipientAddress = "01012345678",
                payloadJson = "{\"name\":\"홍길동\"}",
                scheduledAt = endAtA.minusHours(24),
                recipientUserId = 17,
            ),
        )

        // when
        jobManager.update(job, updateDto(endAtB), now.plusHours(1))
        jobManager.update(job, updateDto(endAtA), now.plusHours(2))

        // then
        val aSchedule = checkNotNull(scheduleRepository.findByJobIdAndRecruitmentEndAt(jobId, endAtA))
        val oldNotifications = notificationRepository.findAllByDeduplicationKeyIn(listOf(oldKey))
        assertEquals(firstSchedule.id, aSchedule.id)
        assertEquals(1, scheduleRepository.findAll().count { it.jobId == jobId && it.recruitmentEndAt == endAtA })
        assertEquals(JobBookmarkReminderScheduleStatus.PENDING, aSchedule.status)
        assertEquals(0, oldNotifications.size)
    }

    private fun updateDto(recruitmentEndAt: LocalDateTime) = JobUpdateDto(
        companyName = "오늘의공고",
        title = "백엔드 개발자",
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.NEWCOMER,
        recruitmentType = JobRecruitmentType.PERIOD,
        recruitmentEndAt = recruitmentEndAt,
    )
}
