package com.ogonggo.core.job.domain

import org.junit.jupiter.api.Assertions.assertEquals
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
            companyLogoUrl = "https://example.com/logo2.png",
            jobField = "서버 개발",
            coverImageUrl = "https://example.com/cover2.png",
            recruitmentHeadcount = 5,
            experienceType = ExperienceType.NEWCOMER,
            experienceMinYears = 0,
            experienceMaxYears = 1,
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
            sourceUrl = "https://example.com/jobs/2",
        )

        assertEquals("변경 회사", job.companyName)
        assertEquals("변경 모회사", job.parentCompanyName)
        assertEquals("https://example.com/logo2.png", job.companyLogoUrl)
        assertEquals("백엔드 인턴", job.title)
        assertEquals("서버 개발", job.jobField)
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
        assertThrows(IllegalArgumentException::class.java) { createJob(companyLogoUrl = " ") }
        assertThrows(IllegalArgumentException::class.java) { createJob(jobField = " ") }
        assertThrows(IllegalArgumentException::class.java) { createJob(coverImageUrl = " ") }
        assertThrows(IllegalArgumentException::class.java) { createJob(recruitmentHeadcount = 0) }
        assertThrows(IllegalArgumentException::class.java) { createJob(experienceMinYears = -1) }
        assertThrows(IllegalArgumentException::class.java) {
            createJob(experienceMinYears = 5, experienceMaxYears = 3)
        }
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
        assertThrows(IllegalStateException::class.java) { job.publish() }
        assertThrows(IllegalStateException::class.java) { job.close(firstClosedAt) }
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
        companyName: String = "오공고",
        parentCompanyName: String? = null,
        companyLogoUrl: String? = null,
        jobField: String? = null,
        coverImageUrl: String? = null,
        recruitmentHeadcount: Int? = null,
        region: String? = "서울",
        experienceMinYears: Int? = 1,
        experienceMaxYears: Int? = 3,
        recruitmentStartAt: LocalDateTime? = LocalDateTime.of(2026, 8, 1, 0, 0),
        recruitmentEndAt: LocalDateTime? = LocalDateTime.of(2026, 8, 31, 23, 59),
    ): Job = Job(
        companyName = companyName,
        parentCompanyName = parentCompanyName,
        companyLogoUrl = companyLogoUrl,
        title = "백엔드 개발자",
        jobField = jobField,
        coverImageUrl = coverImageUrl,
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.EXPERIENCED,
        experienceMinYears = experienceMinYears,
        experienceMaxYears = experienceMaxYears,
        educationLevel = EducationLevel.ANY,
        region = region,
        recruitmentType = JobRecruitmentType.PERIOD,
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
