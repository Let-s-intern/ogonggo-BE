package com.ogonggo.core.job.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobApplicationStatus
import com.ogonggo.core.job.domain.JobBookmark
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.job.persistence.JobBookmarkReminderQueryRepository
import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.contentreview.implement.ContentRejectionManager
import com.ogonggo.core.user.domain.User
import com.ogonggo.core.user.domain.UserProfile
import com.ogonggo.core.user.domain.UserRole
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
    JobBookmarkManager::class,
    JobBookmarkReminderReader::class,
    JobBookmarkReminderNotificationListener::class,
    NotificationAppender::class,
    NotificationManager::class,
    JobBookmarkReminderQueryRepository::class,
    ContentRejectionManager::class,
)
internal class JobBookmarkReminderCandidatePersistenceTest @Autowired constructor(
    private val jobAppender: JobAppender,
    private val reminderReader: JobBookmarkReminderReader,
    private val jobManager: JobManager,
    private val notificationAppender: NotificationAppender,
    private val entityManager: EntityManager,
    private val jobBookmarkManager: JobBookmarkManager,
) {
    private var nextLetsCareerUserId = 200L

    @Test
    @DisplayName("D-1 예정 시각에 활성 일반회원의 유효한 SCRAPPED·PREPARING 스크랩만 대상으로 읽는다")
    fun `발송 가능한 활성 스크랩 대상만 조회한다`() {
        // given
        val registeredAt = LocalDateTime.of(2026, 10, 4, 9, 0)
        val recruitmentEndAt = LocalDateTime.of(2026, 10, 7, 21, 0)
        val job = jobAppender.append(
            JobAppendDto(
                companyName = "오늘의공고",
                title = "백엔드 개발자",
                employmentType = JobEmploymentType.FULL_TIME,
                experienceType = JobExperienceType.NEWCOMER,
                recruitmentType = JobRecruitmentType.PERIOD,
                recruitmentEndAt = recruitmentEndAt,
                publicationStatus = JobPublicationStatus.PUBLISHED,
            ),
            registeredAt,
        )
        val jobId = checkNotNull(job.id)
        val scrappedUserId = appendUser("010-1234-5678", "홍길동")
        val preparingUserId = appendUser("01012345678", "준비중")
        val excludedStatuses = listOf(
            JobApplicationStatus.APPLIED,
            JobApplicationStatus.INTERVIEWING,
            JobApplicationStatus.PASSED,
            JobApplicationStatus.FAILED,
        ).mapIndexed { index, status ->
            appendUser("0101234567${index}", "제외$index") to status
        }
        val suspendedUserId = appendUser("01012345000", "정지회원", active = false)
        val companyUserId = appendUser("01012345001", "기업회원", role = UserRole.COMPANY)
        val deletedBookmarkUserId = appendUser("01012345002", "해제회원")
        val rebookmarkedAfterDueUserId = appendUser("01012345003", "늦은재스크랩")
        val missingProfileUser = User.ofLetsCareer(999, registeredAt)
        entityManager.persist(missingProfileUser)
        entityManager.flush()
        val missingProfileUserId = checkNotNull(missingProfileUser.id)
        val scheduleAt = recruitmentEndAt.minusHours(24)
        (listOf(
            scrappedUserId,
            preparingUserId,
            suspendedUserId,
            companyUserId,
            deletedBookmarkUserId,
            rebookmarkedAfterDueUserId,
            missingProfileUserId,
        ) +
            excludedStatuses.map { it.first }).forEach { userId ->
            entityManager.persist(
                JobBookmark(jobId = jobId, userId = userId, activeSince = registeredAt),
            )
        }
        entityManager.flush()
        jobBookmarkManager.changeApplicationStatus(
            preparingUserId,
            jobId,
            JobApplicationStatus.PREPARING,
            scheduleAt.plusMinutes(1),
        )
        jobBookmarkManager.delete(deletedBookmarkUserId, jobId, registeredAt)
        jobBookmarkManager.delete(rebookmarkedAfterDueUserId, jobId, scheduleAt.plusMinutes(1))
        jobBookmarkManager.append(rebookmarkedAfterDueUserId, jobId, scheduleAt.plusMinutes(2))
        excludedStatuses.forEach { (userId, status) ->
            jobBookmarkManager.changeApplicationStatus(userId, jobId, status, registeredAt)
        }
        assertTrue(reminderReader.readEligibleCandidates(scheduleAt.minusNanos(1), null, limit = 100).isEmpty())

        // when
        val candidates = reminderReader.readEligibleCandidates(scheduleAt, null, limit = 100)

        // then
        assertEquals(setOf(scrappedUserId, preparingUserId), candidates.map { it.userId }.toSet())
        assertEquals("홍길동", candidates.first { it.userId == scrappedUserId }.recipientName)
        assertEquals("010-1234-5678", candidates.first { it.userId == scrappedUserId }.recipientNo)
        assertTrue(candidates.all { it.postingTitle == "백엔드 개발자" })

        // 자동 마감 작업 전이라 RECRUITING이 남아 있어도 실제 마감 시각을 넘으면 신규 적재하지 않는다.
        assertTrue(
            reminderReader.readEligibleCandidates(
                recruitmentEndAt.plusSeconds(1),
                afterBookmarkId = null,
                limit = 100,
            ).isEmpty(),
        )
    }

    @Test
    @DisplayName("이미 알림으로 적재한 수신자는 재실행에서 건너뛰고 이후 수신자를 다시 조회한다")
    fun `알림 행을 진행 기록으로 사용해 재실행을 이어간다`() {
        // given
        val registeredAt = LocalDateTime.of(2026, 10, 4, 9, 0)
        val scheduledAt = registeredAt.plusDays(1)
        val recruitmentEndAt = scheduledAt.plusHours(24)
        val job = jobAppender.append(
            JobAppendDto(
                companyName = "오늘의공고",
                title = "백엔드 개발자",
                employmentType = JobEmploymentType.FULL_TIME,
                experienceType = JobExperienceType.NEWCOMER,
                recruitmentType = JobRecruitmentType.PERIOD,
                recruitmentEndAt = recruitmentEndAt,
                publicationStatus = JobPublicationStatus.PUBLISHED,
            ),
            registeredAt,
        )
        val jobId = checkNotNull(job.id)
        val firstUserId = appendUser("01012345101", "첫 번째 대상")
        val changedLaterUserId = appendUser("01012345102", "커서 통과 후 대상이 될 회원")
        val lastUserId = appendUser("01012345103", "마지막 대상")
        listOf(firstUserId, changedLaterUserId, lastUserId).forEach { userId ->
            entityManager.persist(JobBookmark(jobId = jobId, userId = userId, activeSince = registeredAt))
        }
        entityManager.flush()
        jobBookmarkManager.changeApplicationStatus(
            changedLaterUserId,
            jobId,
            JobApplicationStatus.APPLIED,
            scheduledAt,
        )
        // when: 첫 번째 대상 알림을 적재한다. 별도 일정 커서 대신 이 행이 재실행 기준이 된다.
        val firstRunCandidates = reminderReader.readEligibleCandidates(scheduledAt, null, limit = 1)
        assertEquals(listOf(firstUserId), firstRunCandidates.map { it.userId })
        val firstCandidate = firstRunCandidates.single()
        notificationAppender.append(
            NotificationAppendDto(
                deduplicationKey = JobBookmarkReminderNotificationKey.forRecipient(
                    jobId = jobId,
                    userId = firstUserId,
                    channel = NotificationChannel.KAKAO.name,
                    reminderAt = firstCandidate.reminderAt,
                ),
                channel = NotificationChannel.KAKAO,
                templateCode = "clip_remind",
                recipientAddress = "01012345101",
                payloadJson = "{}",
                scheduledAt = firstCandidate.reminderAt,
                recipientUserId = firstUserId,
            ),
        )

        // 중간 스크랩이 대상 상태로 바뀌면 다음 실행에서 그 수신자를 포함한다.
        jobBookmarkManager.changeApplicationStatus(
            changedLaterUserId,
            jobId,
            JobApplicationStatus.PREPARING,
            scheduledAt.plusMinutes(1),
        )
        val nextRunCandidates = reminderReader.readEligibleCandidates(
            scheduledAt.plusMinutes(1),
            afterBookmarkId = null,
            limit = 10,
        )

        // then
        assertEquals(listOf(changedLaterUserId, lastUserId), nextRunCandidates.map { it.userId })
        assertTrue(nextRunCandidates.none { it.userId == firstUserId })
    }

    @Test
    @DisplayName("D-1이 지났어도 실제 마감 전이면 기존 활성 스크랩을 대상으로 조회한다")
    fun `D-1 경과 후 마감 전까지 기존 스크랩을 조회한다`() {
        // given
        val registeredAt = LocalDateTime.of(2026, 10, 4, 9, 0)
        val now = LocalDateTime.of(2026, 10, 7, 9, 0)
        val initialRecruitmentEndAt = now.plusDays(3)
        val job = jobAppender.append(
            JobAppendDto(
                companyName = "오늘의공고",
                title = "백엔드 개발자",
                employmentType = JobEmploymentType.FULL_TIME,
                experienceType = JobExperienceType.NEWCOMER,
                recruitmentType = JobRecruitmentType.PERIOD,
                recruitmentEndAt = initialRecruitmentEndAt,
                publicationStatus = JobPublicationStatus.PUBLISHED,
            ),
            registeredAt,
        )
        val recruitmentEndAt = now.plusHours(12)
        val userId = appendUser("01012345678", "기존 스크랩 회원")
        entityManager.persist(
            JobBookmark(
                jobId = checkNotNull(job.id),
                userId = userId,
                activeSince = recruitmentEndAt.minusHours(30),
            ),
        )
        entityManager.flush()
        jobManager.update(
            job,
            JobUpdateDto(
                companyName = "오늘의공고",
                title = "백엔드 개발자",
                employmentType = JobEmploymentType.FULL_TIME,
                experienceType = JobExperienceType.NEWCOMER,
                recruitmentType = JobRecruitmentType.PERIOD,
                recruitmentEndAt = recruitmentEndAt,
            ),
            now,
        )

        // when
        val candidates = reminderReader.readEligibleCandidates(now, null, limit = 10)

        // then
        assertEquals(listOf(userId), candidates.map { it.userId })
        assertEquals(recruitmentEndAt.minusHours(24), candidates.single().reminderAt)
    }

    private fun appendUser(
        phoneNum: String,
        name: String,
        role: UserRole = UserRole.USER,
        active: Boolean = true,
    ): Long {
        val user = if (role == UserRole.USER) {
            User.ofLetsCareer(nextLetsCareerUserId++, LocalDateTime.of(2026, 10, 4, 9, 0))
        } else {
            User.ofCompany("company${nextLetsCareerUserId++}@example.com", "encoded", LocalDateTime.of(2026, 10, 4, 9, 0))
        }
        if (!active) user.suspend()
        entityManager.persist(user)
        entityManager.flush()
        val userId = checkNotNull(user.id)
        entityManager.persist(
            UserProfile(
                userId = userId,
                name = name,
                email = "user$userId@example.com",
                phoneNum = phoneNum,
                lastSyncedAt = LocalDateTime.of(2026, 10, 4, 9, 0),
            ),
        )
        entityManager.flush()
        return userId
    }
}
