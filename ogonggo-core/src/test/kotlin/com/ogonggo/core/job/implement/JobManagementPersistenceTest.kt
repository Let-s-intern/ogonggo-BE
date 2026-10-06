package com.ogonggo.core.job.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobManagementSearchCondition
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.job.persistence.JobQueryRepository
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus
import com.ogonggo.core.review.implement.ContentRejectionManager
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    JobReader::class,
    JobQueryRepository::class,
    JobAppender::class,
    JobManager::class,
    ContentRejectionManager::class,
)
internal class JobManagementPersistenceTest @Autowired constructor(
    private val jobReader: JobReader,
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
) {

    @Test
    fun `관리 목록은 게시 상태와 무관하게 미삭제 공고를 읽고 필터를 AND로 묶는다`() {
        val crawledPublished = jobAppender.append(command(publicationStatus = JobPublicationStatus.PUBLISHED))
        val companyPending = jobAppender.append(command(ownerUserId = OWNER_ID))
        val companyApproved = jobAppender.append(command(ownerUserId = OWNER_ID))
        jobManager.approveReview(companyApproved, NOW)
        val crawledHidden = jobAppender.append(command(publicationStatus = JobPublicationStatus.HIDDEN))
        val deleted = jobAppender.append(command(publicationStatus = JobPublicationStatus.PUBLISHED))
        jobManager.delete(deleted, NOW)

        assertEquals(
            listOf(crawledHidden.id, companyApproved.id, companyPending.id, crawledPublished.id),
            ids(JobManagementSearchCondition.NONE),
        )
        assertEquals(listOf(companyApproved.id, crawledPublished.id), ids(JobManagementSearchCondition(published = true)))
        assertEquals(listOf(crawledHidden.id, companyPending.id), ids(JobManagementSearchCondition(published = false)))
        assertEquals(
            listOf(companyApproved.id, companyPending.id),
            ids(JobManagementSearchCondition(source = ContentSource.COMPANY)),
        )
        assertEquals(listOf(companyPending.id), ids(JobManagementSearchCondition(reviewStatus = ReviewStatus.PENDING)))
        assertEquals(
            listOf(crawledHidden.id),
            ids(JobManagementSearchCondition(source = ContentSource.CRAWLER, published = false)),
        )
    }

    @Test
    fun `직군만 거르면 그 직군의 직무 공고도 걸리고 직무는 여러 개 중 하나라도 맞으면 걸린다`() {
        val backend = jobAppender.append(command(jobField = JobField.IT_DEVELOPMENT, jobRole = JobRole.IT_BACKEND))
        val frontend = jobAppender.append(command(jobField = JobField.IT_DEVELOPMENT, jobRole = JobRole.IT_FRONTEND))
        val fieldOnly = jobAppender.append(command(jobField = JobField.IT_DEVELOPMENT))
        jobAppender.append(command(jobField = JobField.DESIGN, jobRole = JobRole.DESIGN_WEB))
        jobAppender.append(command())

        assertEquals(
            listOf(fieldOnly.id, frontend.id, backend.id),
            ids(JobManagementSearchCondition(jobField = JobField.IT_DEVELOPMENT)),
        )
        assertEquals(
            listOf(frontend.id, backend.id),
            ids(JobManagementSearchCondition(jobRoles = setOf(JobRole.IT_BACKEND, JobRole.IT_FRONTEND))),
        )
    }

    @Test
    fun `등록할 때 종료 일시가 지났으면 마감으로 저장하고 종료 시각까지는 모집 중이다`() {
        val endsNow = jobAppender.append(command(recruitmentEndAt = NOW), NOW)
        // DB 일시 칼럼은 마이크로초까지만 저장하므로 나노초 차이로는 경계를 확인할 수 없다.
        val expired = jobAppender.append(command(recruitmentEndAt = NOW.minusSeconds(1)), NOW)
        val closed = jobAppender.append(command(recruitmentEndAt = NOW.plusDays(7)), NOW)
        jobManager.close(closed, NOW.minusDays(1))
        val alwaysOpen = jobAppender.append(command(recruitmentType = JobRecruitmentType.ALWAYS_OPEN), NOW)

        assertEquals(
            listOf(alwaysOpen.id, endsNow.id),
            ids(JobManagementSearchCondition(recruitmentStatus = JobRecruitmentStatus.RECRUITING)),
        )
        assertEquals(
            listOf(closed.id, expired.id),
            ids(JobManagementSearchCondition(recruitmentStatus = JobRecruitmentStatus.CLOSED)),
        )
    }

    @Test
    fun `자동 마감은 종료 일시가 지난 모집 중 공고만 마감하고 직접 마감한 것이 아니므로 마감 처리 일시를 남기지 않는다`() {
        val registeredAt = NOW.minusDays(1)
        val expired = jobAppender.append(command(recruitmentEndAt = NOW.minusSeconds(1)), registeredAt)
        val endsNow = jobAppender.append(command(recruitmentEndAt = NOW), registeredAt)
        val alwaysOpen = jobAppender.append(command(recruitmentType = JobRecruitmentType.ALWAYS_OPEN), registeredAt)
        val deleted = jobAppender.append(command(recruitmentEndAt = NOW.minusSeconds(1)), registeredAt)
        jobManager.delete(deleted, registeredAt)
        assertEquals(
            listOf(alwaysOpen.id, endsNow.id, expired.id),
            ids(JobManagementSearchCondition(recruitmentStatus = JobRecruitmentStatus.RECRUITING)),
        )

        val closedCount = jobManager.closeExpired(NOW)

        assertEquals(1, closedCount)
        jobReader.read(checkNotNull(expired.id)).let {
            assertEquals(JobRecruitmentStatus.CLOSED, it.recruitmentStatus)
            assertNull(it.closedAt)
        }
        assertEquals(
            listOf(alwaysOpen.id, endsNow.id),
            ids(JobManagementSearchCondition(recruitmentStatus = JobRecruitmentStatus.RECRUITING)),
        )
        assertEquals(JobRecruitmentStatus.RECRUITING, jobReader.readIncludingDeleted(checkNotNull(deleted.id)).recruitmentStatus)
    }

    @Test
    fun `수정하면 모집 상태를 다시 정해 종료 일시를 늘린 공고는 모집 중이 되고 직접 마감한 공고는 마감으로 남는다`() {
        val expired = jobAppender.append(command(recruitmentEndAt = NOW.minusDays(1)), NOW)
        val closed = jobAppender.append(command(recruitmentEndAt = NOW.plusDays(1)), NOW)
        jobManager.close(closed, NOW)
        val extended = updateCommand(recruitmentEndAt = NOW.plusDays(7))

        jobManager.update(expired, extended, NOW)
        jobManager.update(closed, extended, NOW)

        assertEquals(JobRecruitmentStatus.RECRUITING, jobReader.read(checkNotNull(expired.id)).recruitmentStatus)
        assertEquals(JobRecruitmentStatus.CLOSED, jobReader.read(checkNotNull(closed.id)).recruitmentStatus)
    }

    @Test
    fun `검색어는 제목과 회사명을 대소문자 없이 찾고 마지막 페이지를 넘으면 빈 목록이다`() {
        val byTitle = jobAppender.append(command(title = "iOS 개발자"))
        val byCompany = jobAppender.append(command(companyName = "IOS컴퍼니"))
        jobAppender.append(command())

        assertEquals(listOf(byCompany.id, byTitle.id), ids(JobManagementSearchCondition(keyword = "ios")))

        val beyond = jobReader.readManagementPage(JobManagementSearchCondition.NONE, JobSortType.LATEST, 5, 20)
        assertEquals(emptyList<Long>(), beyond.jobs.map { it.id })
        assertEquals(3L, beyond.totalElements)
    }

    @Test
    fun `삭제 조회는 이미 삭제된 공고도 찾아 삭제를 멱등하게 만든다`() {
        val job = jobAppender.append(command())
        val jobId = checkNotNull(job.id)
        jobManager.delete(jobReader.readForDelete(jobId), NOW)

        jobManager.delete(jobReader.readForDelete(jobId), NOW.plusDays(1))

        assertEquals(NOW, jobReader.readIncludingDeleted(jobId).deletedAt)
    }

    @Test
    fun `여러 건 잠금 조회는 같은 식별자를 한 번만 읽고 없거나 삭제된 공고가 섞이면 그 식별자를 알려 거절한다`() {
        val first = checkNotNull(jobAppender.append(command()).id)
        val second = checkNotNull(jobAppender.append(command()).id)
        val deleted = jobAppender.append(command())
        jobManager.delete(deleted, NOW)

        assertEquals(listOf(first, second), jobReader.readAllForUpdate(listOf(second, first, second)).map { it.id })
        listOf(checkNotNull(deleted.id), 999_999L).forEach { invalidJobId ->
            val exception = assertThrows(EntityNotFoundException::class.java) {
                jobReader.readAllForUpdate(listOf(first, invalidJobId))
            }
            assertEquals(JobErrorCode.JOB_NOT_FOUND, exception.errorCode)
            assertEquals("일자리 공고를 찾을 수 없습니다. (id: $invalidJobId)", exception.message)
        }
        val exception = assertThrows(EntityNotFoundException::class.java) {
            jobReader.readAllForUpdate(listOf(999_999L, first, checkNotNull(deleted.id)))
        }
        assertEquals("일자리 공고를 찾을 수 없습니다. (id: ${deleted.id}, 999999)", exception.message)
    }

    private fun ids(condition: JobManagementSearchCondition): List<Long?> =
        jobReader.readManagementPage(condition, JobSortType.LATEST, 0, 20).jobs.map { it.id }

    private fun command(
        ownerUserId: Long? = null,
        publicationStatus: JobPublicationStatus = JobPublicationStatus.DRAFT,
        recruitmentType: JobRecruitmentType = JobRecruitmentType.PERIOD,
        recruitmentEndAt: LocalDateTime? = null,
        title: String = "백엔드 개발자",
        companyName: String = "오공고",
        jobField: JobField? = null,
        jobRole: JobRole? = null,
    ): JobAppendDto = JobAppendDto(
        ownerUserId = ownerUserId,
        companyName = companyName,
        title = title,
        jobField = jobField,
        jobRole = jobRole,
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.EXPERIENCED,
        recruitmentType = recruitmentType,
        recruitmentEndAt = recruitmentEndAt,
        publicationStatus = publicationStatus,
    )

    private fun updateCommand(recruitmentEndAt: LocalDateTime): JobUpdateDto = JobUpdateDto(
        companyName = "오공고",
        title = "백엔드 개발자",
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.EXPERIENCED,
        recruitmentType = JobRecruitmentType.PERIOD,
        recruitmentEndAt = recruitmentEndAt,
    )

    companion object {
        private const val OWNER_ID = 7L
        private val NOW = LocalDateTime.of(2026, 9, 14, 12, 0)
    }
}
