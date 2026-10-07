package com.ogonggo.core.job.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationStatus
import com.ogonggo.core.job.domain.JobBookmark
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.persistence.JobBookmarkReminderQueryRepository
import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.review.implement.ContentRejectionManager
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
    JobBookmarkManager::class,
    JobBookmarkReminderReader::class,
    JobBookmarkReminderScheduleListener::class,
    JobBookmarkReminderScheduleManager::class,
    NotificationManager::class,
    JobBookmarkReminderQueryRepository::class,
    ContentRejectionManager::class,
)
internal class JobBookmarkReminderCandidatePersistenceTest @Autowired constructor(
    private val jobAppender: JobAppender,
    private val reminderReader: JobBookmarkReminderReader,
    private val scheduleManager: JobBookmarkReminderScheduleManager,
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
                employmentType = EmploymentType.FULL_TIME,
                experienceType = ExperienceType.NEWCOMER,
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
        assertTrue(scheduleManager.readDueSchedules(scheduleAt.minusNanos(1), limit = 10).isEmpty())
        val dueSchedule = scheduleManager.readDueSchedules(scheduleAt, limit = 10).single()

        // when
        val candidates = reminderReader.readEligibleCandidates(dueSchedule, scheduleAt, limit = 100)

        // then
        assertEquals(setOf(scrappedUserId, preparingUserId), candidates.map { it.userId }.toSet())
        assertEquals("홍길동", candidates.first { it.userId == scrappedUserId }.recipientName)
        assertEquals("010-1234-5678", candidates.first { it.userId == scrappedUserId }.recipientNo)
        assertTrue(candidates.all { it.postingTitle == "백엔드 개발자" })

        // 자동 마감 작업 전이라 RECRUITING이 남아 있어도 실제 마감 시각을 넘으면 신규 적재하지 않는다.
        val expiredSchedule = scheduleManager.readDueSchedules(recruitmentEndAt.plusSeconds(1), 10).single()
        assertTrue(reminderReader.readEligibleCandidates(expiredSchedule, recruitmentEndAt.plusSeconds(1), 100).isEmpty())
    }

    @Test
    @DisplayName("커서가 지난 제외 스크랩은 나중에 대상 상태가 되어도 같은 일정에서 다시 읽지 않는다")
    fun `커서를 지난 스크랩 상태가 바뀌어도 재평가하지 않는다`() {
        // given
        val registeredAt = LocalDateTime.of(2026, 10, 4, 9, 0)
        val scheduledAt = registeredAt.plusDays(1)
        val recruitmentEndAt = scheduledAt.plusHours(24)
        val job = jobAppender.append(
            JobAppendDto(
                companyName = "오늘의공고",
                title = "백엔드 개발자",
                employmentType = EmploymentType.FULL_TIME,
                experienceType = ExperienceType.NEWCOMER,
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
        val schedule = scheduleManager.readDueSchedules(scheduledAt, limit = 10).single()

        // when: APPLIED 상태의 중간 스크랩은 제외되고, 뒤의 적격 스크랩까지 커서가 진행한다.
        val firstPage = reminderReader.readEligibleCandidates(schedule, scheduledAt, limit = 2)
        assertEquals(listOf(firstUserId, lastUserId), firstPage.map { it.userId })
        val lastBookmarkIdInPage = firstPage.last().bookmarkId
        scheduleManager.advanceSchedule(
            scheduleId = schedule.id,
            lastBookmarkId = lastBookmarkIdInPage,
            isComplete = false,
        )

        // 커서가 중간 스크랩을 지난 뒤 그 상태가 PREPARING으로 바뀐다.
        jobBookmarkManager.changeApplicationStatus(
            changedLaterUserId,
            jobId,
            JobApplicationStatus.PREPARING,
            scheduledAt.plusMinutes(1),
        )
        val resumedSchedule = scheduleManager.readDueSchedules(scheduledAt.plusMinutes(1), limit = 10).single()
        val nextPage = reminderReader.readEligibleCandidates(resumedSchedule, scheduledAt.plusMinutes(1), limit = 2)

        // then
        assertTrue(nextPage.isEmpty())
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
