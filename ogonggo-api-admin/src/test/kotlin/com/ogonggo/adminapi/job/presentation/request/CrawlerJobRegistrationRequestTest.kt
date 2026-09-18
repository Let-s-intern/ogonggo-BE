package com.ogonggo.adminapi.job.presentation.request

import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobRecruitmentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class CrawlerJobRegistrationRequestTest {

    @Test
    fun `크롤러가 고른 판단 값을 서버가 다른 값으로 바꾸지 않는다`() {
        // 예전에는 모집 일시와 경력 연수가 없으면 상시 채용·경력 무관으로 바꿨다. 이제는 크롤러가 보낸 그대로다.
        val command = request(
            experienceType = ExperienceType.NEWCOMER,
            educationLevel = EducationLevel.HIGH_SCHOOL,
            recruitmentType = JobRecruitmentType.PERIOD,
        ).toCommand().job

        assertEquals(ExperienceType.NEWCOMER, command.experienceType)
        assertEquals(null, command.experienceMinYears)
        assertEquals(EducationLevel.HIGH_SCHOOL, command.educationLevel)
        assertEquals(JobRecruitmentType.PERIOD, command.recruitmentType)
        assertEquals(null, command.recruitmentStartAt)
        assertEquals(null, command.recruitmentEndAt)
    }

    @Test
    fun `등록 API에 새로 받는 칸과 태그를 옮긴다`() {
        val command = request(
            recruitmentHeadcount = 2,
            recruitmentNotice = "제출 서류는 반환하지 않습니다.",
        ).toCommand()

        assertEquals(2, command.job.recruitmentHeadcount)
        assertEquals("https://example.com/logo.png", command.job.coverImageUrl)
        assertEquals(true, command.job.closesWhenFilled)
        assertEquals(false, command.job.autoCloseEnabled)
        assertEquals("제출 서류는 반환하지 않습니다.", command.job.recruitmentNotice)
        assertEquals(JobApplicationMethod.EMAIL, command.job.applicationMethod)
        assertEquals(listOf("백엔드"), command.tags)
    }

    @Test
    fun `지원 접수 이메일과 문의 이메일을 따로 옮긴다`() {
        // 한 공고에 둘 다 적힐 수 있다. 어느 한쪽만 있으면 다른 칸은 비워 둔다.
        val both = request(applicationEmail = "recruit@example.com", inquiryEmail = "hr@example.com").toCommand().job
        val inquiryOnly = request(inquiryEmail = "hr@example.com").toCommand().job

        assertEquals("recruit@example.com", both.applicationEmail)
        assertEquals("hr@example.com", both.inquiryEmail)
        assertEquals(null, inquiryOnly.applicationEmail)
        assertEquals("hr@example.com", inquiryOnly.inquiryEmail)
    }

    @Test
    fun `상시 채용에 모집 종료 일시가 있으면 어느 필드가 틀렸는지 알린다`() {
        val exception = assertThrows<InvalidRequestFieldException> {
            request(recruitmentEndAt = LocalDateTime.of(2026, 9, 30, 23, 59, 59)).toCommand()
        }

        assertEquals("recruitmentEndAt", exception.fieldName)
    }

    @Test
    fun `교체 요청은 등록 요청과 같은 칸을 옮긴다`() {
        val replace = CrawlerJobReplaceRequest(
            companyName = "오공고",
            title = "백엔드 개발자",
            employmentType = EmploymentType.CONTRACT,
            experienceType = ExperienceType.BOTH,
            educationLevel = EducationLevel.ANY,
            recruitmentType = JobRecruitmentType.PERIOD,
            recruitmentEndAt = LocalDateTime.of(2026, 9, 30, 23, 59, 59),
            applicationEmail = "recruit@example.com",
            inquiryEmail = "hr@example.com",
            sourceUrl = "https://example.com/jobs/1#2",
        ).toCommand()

        assertEquals(EmploymentType.CONTRACT, replace.employmentType)
        assertEquals(ExperienceType.BOTH, replace.experienceType)
        assertEquals(LocalDateTime.of(2026, 9, 30, 23, 59, 59), replace.recruitmentEndAt)
        assertEquals("recruit@example.com", replace.applicationEmail)
        assertEquals("hr@example.com", replace.inquiryEmail)
        assertEquals("https://example.com/jobs/1#2", replace.sourceUrl)
    }

    private fun request(
        experienceType: ExperienceType = ExperienceType.IRRELEVANT,
        educationLevel: EducationLevel = EducationLevel.ANY,
        recruitmentType: JobRecruitmentType = JobRecruitmentType.ALWAYS_OPEN,
        recruitmentEndAt: LocalDateTime? = null,
        recruitmentHeadcount: Int? = null,
        recruitmentNotice: String? = null,
        applicationEmail: String? = null,
        inquiryEmail: String? = null,
    ) = CrawlerJobRegistrationRequest(
        companyName = "오공고",
        title = "백엔드 개발자",
        coverImageUrl = "https://example.com/logo.png",
        employmentType = EmploymentType.FULL_TIME,
        experienceType = experienceType,
        educationLevel = educationLevel,
        recruitmentType = recruitmentType,
        recruitmentEndAt = recruitmentEndAt,
        recruitmentHeadcount = recruitmentHeadcount,
        closesWhenFilled = true,
        autoCloseEnabled = false,
        recruitmentNotice = recruitmentNotice,
        applicationMethod = JobApplicationMethod.EMAIL,
        applicationEmail = applicationEmail,
        inquiryEmail = inquiryEmail,
        sourceUrl = "https://example.com/jobs/1",
        tags = listOf("백엔드"),
    )
}
