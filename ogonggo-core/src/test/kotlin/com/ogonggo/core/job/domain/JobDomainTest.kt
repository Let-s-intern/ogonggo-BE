package com.ogonggo.core.job.domain

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.review.domain.ReviewStatus
import com.ogonggo.core.review.error.ReviewErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class JobDomainTest {

    @Test
    fun `채용공고 정보를 수정한다`() {
        val job = createJob()
        val startAt = LocalDateTime.of(2026, 9, 1, 0, 0)
        val endAt = startAt.plusDays(30)

        job.update(
            companyName = "변경 회사",
            title = "백엔드 인턴",
            employmentType = EmploymentType.INTERN,
            parentCompanyName = "변경 모회사",
            jobField = "개발",
            jobRole = "서버 개발자",
            industry = "IT",
            coverImageUrl = "https://example.com/cover2.png",
            recruitmentHeadcount = 5,
            experienceType = ExperienceType.NEWCOMER,
            experienceMinYears = 0,
            educationLevel = EducationLevel.BACHELOR,
            region = "부산",
            recruitmentType = JobRecruitmentType.PERIOD,
            recruitmentStartAt = startAt,
            recruitmentEndAt = endAt,
            closesWhenFilled = true,
            autoCloseEnabled = false,
            companyAndTeamIntroduction = "변경된 회사 및 팀 소개",
            responsibilities = "변경된 주요 업무",
            qualifications = "변경된 자격 요건",
            preferredQualifications = "변경된 우대 사항",
            compensation = "변경된 급여 및 처우",
            benefits = "변경된 복지 및 혜택",
            hiringProcess = "변경된 채용 절차",
            recruitmentNotice = "변경된 채용 안내사항",
            applicationMethod = JobApplicationMethod.EMAIL,
            applicationEmail = "recruit@example.com",
            inquiryEmail = "hr@example.com",
            sourceUrl = "https://example.com/jobs/2",
        )

        assertEquals("변경 회사", job.companyName)
        assertEquals("recruit@example.com", job.applicationEmail)
        assertEquals("hr@example.com", job.inquiryEmail)
        assertEquals("변경 모회사", job.parentCompanyName)
        assertEquals("백엔드 인턴", job.title)
        assertEquals("개발", job.jobField)
        assertEquals("서버 개발자", job.jobRole)
        assertEquals("IT", job.industry)
        assertEquals("https://example.com/cover2.png", job.coverImageUrl)
        assertEquals(5, job.recruitmentHeadcount)
        assertEquals(EmploymentType.INTERN, job.employmentType)
        assertEquals(startAt, job.recruitmentStartAt)
        assertEquals(endAt, job.recruitmentEndAt)
        assertEquals("변경된 회사 및 팀 소개", job.companyAndTeamIntroduction)
        assertEquals("변경된 주요 업무", job.responsibilities)
        assertEquals("변경된 자격 요건", job.qualifications)
        assertEquals("변경된 우대 사항", job.preferredQualifications)
        assertEquals("변경된 급여 및 처우", job.compensation)
        assertEquals("변경된 복지 및 혜택", job.benefits)
        assertEquals("변경된 채용 절차", job.hiringProcess)
        assertEquals("변경된 채용 안내사항", job.recruitmentNotice)
        assertEquals(JobApplicationMethod.EMAIL, job.applicationMethod)
        assertEquals(true, job.closesWhenFilled)
        assertEquals(false, job.autoCloseEnabled)
    }

    @Test
    fun `필수값과 경력 및 모집 기간을 검증한다`() {
        assertThrows(IllegalArgumentException::class.java) { createJob(companyName = " ") }
        assertThrows(IllegalArgumentException::class.java) { createJob(parentCompanyName = " ") }
        assertThrows(IllegalArgumentException::class.java) { createJob(region = " ") }
        assertThrows(IllegalArgumentException::class.java) { createJob(jobField = " ") }
        assertThrows(IllegalArgumentException::class.java) { createJob(jobRole = " ") }
        assertThrows(IllegalArgumentException::class.java) { createJob(industry = " ") }
        assertThrows(IllegalArgumentException::class.java) { createJob(coverImageUrl = " ") }
        assertThrows(IllegalArgumentException::class.java) { createJob(recruitmentHeadcount = 0) }
        assertThrows(IllegalArgumentException::class.java) { createJob(experienceMinYears = -1) }
        assertThrows(IllegalArgumentException::class.java) {
            createJob(
                recruitmentStartAt = LocalDateTime.of(2026, 9, 2, 0, 0),
                recruitmentEndAt = LocalDateTime.of(2026, 9, 1, 0, 0),
            )
        }
    }

    @Test
    fun `게시 숨김 마감 보관을 처리한다`() {
        val job = createJob()
        val firstClosedAt = LocalDateTime.of(2026, 9, 1, 0, 0)

        job.publish()
        assertEquals(JobPublicationStatus.PUBLISHED, job.publicationStatus)

        job.hide()
        assertEquals(JobPublicationStatus.HIDDEN, job.publicationStatus)

        job.close(firstClosedAt)
        job.close(firstClosedAt.plusDays(1))
        assertEquals(firstClosedAt, job.closedAt)

        job.archive()
        assertEquals(JobPublicationStatus.ARCHIVED, job.publicationStatus)
        // 보관된 공고는 운영자가 콘솔에서 건드릴 수 있으므로 500이 아니라 409로 알린다.
        assertEquals(JobErrorCode.JOB_ARCHIVED, assertThrows(ConflictException::class.java) { job.publish() }.errorCode)
        assertThrows(ConflictException::class.java) { job.close(firstClosedAt) }
    }

    @Test
    fun `기업회원 공고는 검수 대기로 시작하고 수집한 공고는 검수 상태가 없다`() {
        assertEquals(ReviewStatus.PENDING, createJob(ownerUserId = 7L).reviewStatus)
        assertNull(createJob().reviewStatus)
        assertThrows(IllegalArgumentException::class.java) {
            createJob(ownerUserId = 7L, publicationStatus = JobPublicationStatus.PUBLISHED)
        }
    }

    @Test
    fun `크롤러가 검수를 요청한 수집 공고는 검수 대기로 시작하고 승인해야 게시된다`() {
        val job = createJob(requiresReview = true)

        assertEquals(ReviewStatus.PENDING, job.reviewStatus)
        assertEquals(ReviewErrorCode.REVIEW_NOT_APPROVED, assertThrows(ConflictException::class.java) { job.publish() }.errorCode)
        assertThrows(IllegalArgumentException::class.java) {
            createJob(requiresReview = true, publicationStatus = JobPublicationStatus.PUBLISHED)
        }

        job.approveReview()
        assertEquals(JobPublicationStatus.PUBLISHED, job.publicationStatus)
    }

    @Test
    fun `기업회원 공고는 승인 전에 게시할 수 없고 승인하면 곧바로 게시된다`() {
        val job = createJob(ownerUserId = 7L)

        val exception = assertThrows(ConflictException::class.java) { job.publish() }
        assertEquals(ReviewErrorCode.REVIEW_NOT_APPROVED, exception.errorCode)

        job.approveReview()
        assertEquals(ReviewStatus.APPROVED, job.reviewStatus)
        assertEquals(JobPublicationStatus.PUBLISHED, job.publicationStatus)
    }

    @Test
    fun `반려하거나 검수 대기로 되돌리면 노출을 끈다`() {
        val rejected = createJob(ownerUserId = 7L).apply { approveReview() }
        rejected.rejectReview()
        assertEquals(ReviewStatus.REJECTED, rejected.reviewStatus)
        assertEquals(JobPublicationStatus.HIDDEN, rejected.publicationStatus)

        val draft = createJob(ownerUserId = 7L)
        draft.requestReview()
        // 게시한 적 없는 초안은 숨김으로 바꾸지 않고 그대로 둔다.
        assertEquals(JobPublicationStatus.DRAFT, draft.publicationStatus)

        val approved = createJob(ownerUserId = 7L).apply { approveReview() }
        approved.requestReview()
        assertEquals(ReviewStatus.PENDING, approved.reviewStatus)
        assertEquals(JobPublicationStatus.HIDDEN, approved.publicationStatus)
    }

    @Test
    fun `수집한 공고는 검수할 수 없다`() {
        val exception = assertThrows(ConflictException::class.java) { createJob().approveReview() }

        assertEquals(ReviewErrorCode.CONTENT_NOT_REVIEWABLE, exception.errorCode)
    }

    @Test
    fun `제목과 본문은 넘어온 칸만 고치고 null이면 비운다`() {
        val job = createJob()

        job.editContent(
            title = "고친 제목",
            contents = mapOf(JobContentField.RESPONSIBILITIES to "고친 업무", JobContentField.BENEFITS to null),
        )

        assertEquals("고친 제목", job.title)
        assertEquals("고친 업무", job.responsibilities)
        assertNull(job.benefits)
        assertEquals("자격 요건", job.qualifications)

        job.editContent(title = null, contents = emptyMap())
        assertEquals("고친 제목", job.title)

        assertThrows(IllegalArgumentException::class.java) { job.editContent(title = " ", contents = emptyMap()) }
        assertThrows(IllegalArgumentException::class.java) {
            job.editContent(title = null, contents = mapOf(JobContentField.COMPENSATION to " "))
        }
    }

    @Test
    fun `모집 상태는 마감 처리와 종료 일시로 계산하며 종료 시각까지는 모집 중이다`() {
        val endAt = LocalDateTime.of(2026, 9, 14, 23, 59)
        val job = createJob(recruitmentStartAt = endAt.minusDays(7), recruitmentEndAt = endAt)

        assertEquals(JobRecruitmentStatus.RECRUITING, job.recruitmentStatus(endAt))
        assertEquals(JobRecruitmentStatus.CLOSED, job.recruitmentStatus(endAt.plusNanos(1)))

        val alwaysOpen = createJob(recruitmentType = JobRecruitmentType.ALWAYS_OPEN)
        assertEquals(JobRecruitmentStatus.RECRUITING, alwaysOpen.recruitmentStatus(endAt))
        alwaysOpen.close(endAt)
        assertEquals(JobRecruitmentStatus.CLOSED, alwaysOpen.recruitmentStatus(endAt))
    }

    @Test
    fun `상시 채용에는 모집 종료 일시를 둘 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) {
            createJob(recruitmentType = JobRecruitmentType.ALWAYS_OPEN, recruitmentEndAt = LocalDateTime.of(2026, 9, 1, 0, 0))
        }
    }

    @Test
    fun `삭제는 멱등하며 삭제 후 변경을 차단한다`() {
        val job = createJob()
        val firstDeletedAt = LocalDateTime.of(2026, 9, 1, 0, 0)

        job.delete(firstDeletedAt)
        job.delete(firstDeletedAt.plusDays(1))

        assertEquals(firstDeletedAt, job.deletedAt)
        assertThrows(IllegalStateException::class.java) { job.publish() }
        assertThrows(IllegalStateException::class.java) { job.archive() }
    }

    private fun createJob(
        ownerUserId: Long? = null,
        recruitmentType: JobRecruitmentType = JobRecruitmentType.PERIOD,
        publicationStatus: JobPublicationStatus = JobPublicationStatus.DRAFT,
        companyName: String = "오공고",
        parentCompanyName: String? = null,
        requiresReview: Boolean = false,
        jobField: String? = null,
        jobRole: String? = null,
        industry: String? = null,
        coverImageUrl: String? = null,
        recruitmentHeadcount: Int? = null,
        region: String? = "서울",
        experienceMinYears: Int? = 1,
        recruitmentStartAt: LocalDateTime? = LocalDateTime.of(2026, 8, 1, 0, 0),
        // 상시 채용은 종료 일시를 둘 수 없으므로 기본값도 모집 유형을 따른다.
        recruitmentEndAt: LocalDateTime? =
            if (recruitmentType == JobRecruitmentType.ALWAYS_OPEN) null else LocalDateTime.of(2026, 8, 31, 23, 59),
    ): Job = Job(
        ownerUserId = ownerUserId,
        publicationStatus = publicationStatus,
        companyName = companyName,
        parentCompanyName = parentCompanyName,
        requiresReview = requiresReview,
        title = "백엔드 개발자",
        jobField = jobField,
        jobRole = jobRole,
        industry = industry,
        coverImageUrl = coverImageUrl,
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.EXPERIENCED,
        experienceMinYears = experienceMinYears,
        educationLevel = EducationLevel.ANY,
        region = region,
        recruitmentType = recruitmentType,
        recruitmentHeadcount = recruitmentHeadcount,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = recruitmentEndAt,
        companyAndTeamIntroduction = "회사 및 팀 소개",
        responsibilities = "주요 업무",
        qualifications = "자격 요건",
        preferredQualifications = "우대 사항",
        compensation = "급여 및 처우",
        benefits = "복지 및 혜택",
        hiringProcess = "채용 절차",
        sourceUrl = "https://example.com/jobs/1",
    )
}
