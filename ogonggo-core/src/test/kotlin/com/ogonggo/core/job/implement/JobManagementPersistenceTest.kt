package com.ogonggo.core.job.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobManagementSearchCondition
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.persistence.JobQueryRepository
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus
import com.ogonggo.core.review.implement.ContentRejectionManager
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `모집 상태는 마감 처리와 종료 일시로 계산해 거르며 종료 시각까지는 모집 중이다`() {
        val endsNow = jobAppender.append(command(recruitmentEndAt = NOW))
        // DB 일시 칼럼은 마이크로초까지만 저장하므로 나노초 차이로는 경계를 확인할 수 없다.
        val expired = jobAppender.append(command(recruitmentEndAt = NOW.minusSeconds(1)))
        val closed = jobAppender.append(command(recruitmentEndAt = NOW.plusDays(7)))
        jobManager.close(closed, NOW.minusDays(1))
        val alwaysOpen = jobAppender.append(command(recruitmentType = JobRecruitmentType.ALWAYS_OPEN))

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
    fun `검색어는 제목과 회사명을 대소문자 없이 찾고 마지막 페이지를 넘으면 빈 목록이다`() {
        val byTitle = jobAppender.append(command(title = "iOS 개발자"))
        val byCompany = jobAppender.append(command(companyName = "IOS컴퍼니"))
        jobAppender.append(command())

        assertEquals(listOf(byCompany.id, byTitle.id), ids(JobManagementSearchCondition(keyword = "ios")))

        val beyond = jobReader.readManagementPage(JobManagementSearchCondition.NONE, JobSortType.LATEST, 5, 20, NOW)
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

    private fun ids(condition: JobManagementSearchCondition): List<Long?> =
        jobReader.readManagementPage(condition, JobSortType.LATEST, 0, 20, NOW).jobs.map { it.id }

    private fun command(
        ownerUserId: Long? = null,
        publicationStatus: JobPublicationStatus = JobPublicationStatus.DRAFT,
        recruitmentType: JobRecruitmentType = JobRecruitmentType.PERIOD,
        recruitmentEndAt: LocalDateTime? = null,
        title: String = "백엔드 개발자",
        companyName: String = "오공고",
    ): JobAppendDto = JobAppendDto(
        ownerUserId = ownerUserId,
        companyName = companyName,
        title = title,
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.EXPERIENCED,
        recruitmentType = recruitmentType,
        recruitmentEndAt = recruitmentEndAt,
        publicationStatus = publicationStatus,
    )

    companion object {
        private const val OWNER_ID = 7L
        private val NOW = LocalDateTime.of(2026, 9, 14, 12, 0)
    }
}
