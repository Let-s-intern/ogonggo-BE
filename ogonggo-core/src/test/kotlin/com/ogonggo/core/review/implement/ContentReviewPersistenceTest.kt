package com.ogonggo.core.review.implement

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.implement.BootcampAppender
import com.ogonggo.core.bootcamp.implement.BootcampManager
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.bootcamp.persistence.BootcampQueryRepository
import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.persistence.JobQueryRepository
import com.ogonggo.core.review.domain.ReviewContentType
import com.ogonggo.core.review.domain.ReviewStatus
import com.ogonggo.core.review.error.ReviewErrorCode
import com.ogonggo.core.review.persistence.ContentRejectionJpaRepository
import com.ogonggo.core.review.persistence.ContentRejectionQueryRepository
import java.time.LocalDate
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
    ContentRejectionReader::class,
    ContentRejectionManager::class,
    ContentRejectionQueryRepository::class,
    JobAppender::class,
    JobManager::class,
    JobReader::class,
    JobQueryRepository::class,
    BootcampAppender::class,
    BootcampManager::class,
    BootcampReader::class,
    BootcampQueryRepository::class,
)
internal class ContentReviewPersistenceTest @Autowired constructor(
    private val contentRejectionReader: ContentRejectionReader,
    private val contentRejectionManager: ContentRejectionManager,
    private val contentRejectionRepository: ContentRejectionJpaRepository,
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
    private val jobReader: JobReader,
    private val bootcampAppender: BootcampAppender,
    private val bootcampManager: BootcampManager,
    private val bootcampReader: BootcampReader,
) {

    @Test
    fun `반려하면 기록을 남기고 승인하면 지우며 다시 반려하면 같은 행을 되살린다`() {
        val job = jobAppender.append(companyJob())
        val jobId = checkNotNull(job.id)

        jobManager.rejectReview(job, "급여 조건이 비어 있습니다.", NOW)
        assertEquals("급여 조건이 비어 있습니다.", contentRejectionReader.readActive(ReviewContentType.JOB, jobId).reason)

        jobManager.approveReview(job, NOW.plusHours(1))
        assertEquals(JobPublicationStatus.PUBLISHED, job.publicationStatus)
        val cleared = assertThrows(EntityNotFoundException::class.java) {
            contentRejectionReader.readActive(ReviewContentType.JOB, jobId)
        }
        assertEquals(ReviewErrorCode.REJECTION_NOT_FOUND, cleared.errorCode)

        jobManager.rejectReview(job, "근무지가 비어 있습니다.", NOW.plusHours(2))
        val rejectedAgain = contentRejectionReader.readActive(ReviewContentType.JOB, jobId)
        assertEquals("근무지가 비어 있습니다.", rejectedAgain.reason)
        assertEquals(NOW.plusHours(2), rejectedAgain.rejectedAt)
        assertEquals(JobPublicationStatus.HIDDEN, job.publicationStatus)
        assertEquals(1L, contentRejectionRepository.count())
    }

    @Test
    fun `검수 대기로 되돌리면 반려 기록을 지운다`() {
        val bootcamp = bootcampAppender.append(companyBootcamp())
        val bootcampId = checkNotNull(bootcamp.id)
        bootcampManager.rejectReview(bootcamp, "커리큘럼이 비어 있습니다.", NOW)

        bootcampManager.requestReview(bootcamp, NOW.plusMinutes(1))

        assertEquals(ReviewStatus.PENDING, bootcamp.reviewStatus)
        assertThrows(EntityNotFoundException::class.java) {
            contentRejectionReader.readActive(ReviewContentType.BOOTCAMP, bootcampId)
        }
    }

    @Test
    fun `반려 보관은 최근 반려 순이며 종류와 제목과 사유로 좁히고 삭제된 콘텐츠도 남긴다`() {
        val job = jobAppender.append(companyJob(title = "Content Specialist"))
        val bootcamp = bootcampAppender.append(companyBootcamp(title = "데이터 부트캠프"))
        jobManager.rejectReview(job, "급여 조건이 비어 있습니다.", NOW)
        bootcampManager.rejectReview(bootcamp, "교육 기간을 확인해 주세요.", NOW.plusHours(1))
        jobManager.delete(jobReader.readForDelete(checkNotNull(job.id)), NOW.plusHours(2))

        val all = contentRejectionReader.readActivePage(null, null, 0, 10).rejections
        assertEquals(listOf(ReviewContentType.BOOTCAMP, ReviewContentType.JOB), all.map { it.contentType })
        assertEquals(listOf(true, false), all.map { it.contentExists })
        assertEquals("Content Specialist", all.last().title)

        assertEquals(
            listOf(job.id),
            contentRejectionReader.readActivePage(ReviewContentType.JOB, null, 0, 10).rejections.map { it.contentId },
        )
        assertEquals(
            listOf(job.id),
            contentRejectionReader.readActivePage(null, "급여", 0, 10).rejections.map { it.contentId },
        )
        assertEquals(
            listOf(bootcamp.id),
            contentRejectionReader.readActivePage(null, "데이터", 0, 10).rejections.map { it.contentId },
        )
        assertEquals(1L, contentRejectionReader.readActivePage(null, "CONTENT", 0, 10).totalElements)
    }

    @Test
    fun `사유를 고치면 수정 일시를 남기고 풀린 기록은 고칠 수 없다`() {
        val job = jobAppender.append(companyJob())
        val jobId = checkNotNull(job.id)
        jobManager.rejectReview(job, "급여 조건이 비어 있습니다.", NOW)
        assertNull(contentRejectionReader.readActive(ReviewContentType.JOB, jobId).reasonUpdatedAt)

        contentRejectionManager.replaceReason(ReviewContentType.JOB, jobId, "급여를 적어 주세요.", NOW.plusDays(1))

        val replaced = contentRejectionReader.readActive(ReviewContentType.JOB, jobId)
        assertEquals("급여를 적어 주세요.", replaced.reason)
        assertEquals(NOW, replaced.rejectedAt)
        assertEquals(NOW.plusDays(1), replaced.reasonUpdatedAt)

        jobManager.approveReview(job, NOW.plusDays(2))
        assertThrows(EntityNotFoundException::class.java) {
            contentRejectionManager.replaceReason(ReviewContentType.JOB, jobId, "다른 사유", NOW.plusDays(3))
        }
    }

    @Test
    fun `검수 대기는 등록 순서대로 읽고 판정한 건은 세지 않는다`() {
        val older = jobAppender.append(companyJob())
        val newer = jobAppender.append(companyJob())
        val approved = jobAppender.append(companyJob())
        jobAppender.append(crawledJob())
        jobManager.approveReview(approved, NOW)
        val pendingBootcamp = bootcampAppender.append(companyBootcamp())

        assertEquals(listOf(older.id, newer.id), jobReader.readPendingReviews().map { it.id })
        assertEquals(2L, jobReader.countPendingReviews())
        assertEquals(listOf(pendingBootcamp.id), bootcampReader.readPendingReviews().map { it.id })
        assertEquals(1L, bootcampReader.countPendingReviews())
    }

    private fun companyJob(title: String = "백엔드 개발자"): JobAppendDto = crawledJob(title).copy(ownerUserId = OWNER_ID)

    private fun crawledJob(title: String = "백엔드 개발자"): JobAppendDto = JobAppendDto(
        companyName = "뱅크",
        title = title,
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.EXPERIENCED,
        recruitmentType = JobRecruitmentType.ALWAYS_OPEN,
    )

    private fun companyBootcamp(title: String = "백엔드 부트캠프"): BootcampAppendDto = BootcampAppendDto(
        ownerUserId = OWNER_ID,
        companyName = "오공고 교육사",
        title = title,
        programType = "개발",
        operationType = OperationType.ONLINE,
        recruitmentType = BootcampRecruitmentType.ALWAYS_OPEN,
        programStartDate = LocalDate.of(2026, 10, 1),
        programEndDate = LocalDate.of(2026, 12, 1),
        tuitionType = TuitionType.FREE,
        representativeImageUrl = "https://example.com/images/bootcamp.png",
        shortDescription = "백엔드 개발자로 성장하는 12주",
        content = "부트캠프 상세 내용",
        applicationMethod = ApplicationMethod.EXTERNAL_PAGE,
        applicationUrl = "https://example.com/apply",
    )

    companion object {
        private const val OWNER_ID = 7L
        private val NOW = LocalDateTime.of(2026, 9, 14, 12, 0)
    }
}
