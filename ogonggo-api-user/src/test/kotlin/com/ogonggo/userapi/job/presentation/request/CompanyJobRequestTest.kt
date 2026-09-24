package com.ogonggo.userapi.job.presentation.request

import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.userapi.error.InvalidRequestFieldException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CompanyJobRequestTest {

    @Test
    fun `이메일 지원 주소와 기업 로고를 공고 입력으로 옮긴다`() {
        // when
        val command = request(
            logoUrl = "https://example.com/logo.png",
            applyEmail = "recruit@example.com",
        ).toCommand()

        // then
        assertEquals("https://example.com/logo.png", command.logoUrl)
        assertEquals("recruit@example.com", command.applyEmail)
    }

    @Test
    fun `이메일 지원 주소나 기업 로고가 공백이면 어느 칸이 틀렸는지 알린다`() {
        val cases = mapOf(
            "logoUrl" to { request(logoUrl = "") },
            "applyEmail" to { request(applyEmail = "") },
        )

        cases.forEach { (field, request) ->
            val exception = assertThrows<InvalidRequestFieldException> { request().toCommand() }
            assertEquals(field, exception.fieldName)
        }
    }

    @Test
    fun `이메일 지원을 고르고 지원 이메일을 보내지 않으면 지원 이메일 칸이 틀렸다고 알린다`() {
        // when
        val exception = assertThrows<InvalidRequestFieldException> { request(applyEmail = null).toCommand() }

        // then
        assertEquals("applyEmail", exception.fieldName)
    }

    @Test
    fun `외부 페이지 지원은 지원 이메일 없이 등록할 수 있다`() {
        // when
        val command = request(applicationMethod = JobApplicationMethod.EXTERNAL_PAGE, applyEmail = null).toCommand()

        // then
        assertEquals(null, command.applyEmail)
    }

    private fun request(
        logoUrl: String? = null,
        applicationMethod: JobApplicationMethod = JobApplicationMethod.EMAIL,
        applyEmail: String? = "recruit@example.com",
    ) = CreateCompanyJobRequest(
        companyName = "오공고",
        parentCompanyName = null,
        title = "백엔드 개발자",
        jobField = null,
        jobRole = null,
        industry = null,
        coverImageUrl = null,
        logoUrl = logoUrl,
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.NEWCOMER,
        experienceMinYears = null,
        educationLevel = null,
        region = null,
        recruitmentType = JobRecruitmentType.ALWAYS_OPEN,
        recruitmentHeadcount = null,
        recruitmentStartAt = null,
        recruitmentEndAt = null,
        closesWhenFilled = null,
        autoCloseEnabled = null,
        companyAndTeamIntroduction = null,
        responsibilities = null,
        qualifications = null,
        preferredQualifications = null,
        compensation = null,
        benefits = null,
        hiringProcess = null,
        recruitmentNotice = null,
        applicationMethod = applicationMethod,
        applyEmail = applyEmail,
        sourceUrl = null,
    )
}
