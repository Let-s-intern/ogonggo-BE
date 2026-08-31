package com.ogonggo.adminapi.job.presentation.request

import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CrawlerJobRegistrationRequestTest {

    @Test
    fun `모집 일시와 경력과 학력이 없으면 상시 채용과 무관으로 변환한다`() {
        val command = request().toCommand()

        assertEquals(JobRecruitmentType.ALWAYS_OPEN, command.recruitmentType)
        assertEquals(ExperienceType.IRRELEVANT, command.experienceType)
        assertEquals(EducationLevel.ANY, command.educationLevel)
        assertEquals(null, command.region)
        assertEquals(null, command.parentCompanyName)
    }

    @Test
    fun `모집 일시가 하나라도 있으면 기간 채용으로 변환한다`() {
        val startOnly = request(recruitmentStartAt = LocalDateTime.of(2026, 9, 1, 0, 0)).toCommand()
        val endOnly = request(recruitmentEndAt = LocalDateTime.of(2026, 9, 30, 23, 59)).toCommand()

        assertEquals(JobRecruitmentType.PERIOD, startOnly.recruitmentType)
        assertEquals(JobRecruitmentType.PERIOD, endOnly.recruitmentType)
    }

    @Test
    fun `경력 연수가 있으면 경력으로 변환한다`() {
        val command = request(experienceMinYears = 3).toCommand()

        assertEquals(ExperienceType.EXPERIENCED, command.experienceType)
        assertEquals(3, command.experienceMinYears)
    }

    @Test
    fun `크롤러가 등록한 공고는 게시 상태로 변환한다`() {
        assertEquals(JobPublicationStatus.PUBLISHED, request().toCommand().publicationStatus)
    }

    private fun request(
        experienceMinYears: Int? = null,
        recruitmentStartAt: LocalDateTime? = null,
        recruitmentEndAt: LocalDateTime? = null,
    ) = CrawlerJobRegistrationRequest(
        companyName = "오공고",
        title = "백엔드 개발자",
        employmentType = EmploymentType.FULL_TIME,
        sourceUrl = "https://example.com/jobs/1",
        experienceMinYears = experienceMinYears,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = recruitmentEndAt,
        tags = listOf("백엔드"),
    )
}
