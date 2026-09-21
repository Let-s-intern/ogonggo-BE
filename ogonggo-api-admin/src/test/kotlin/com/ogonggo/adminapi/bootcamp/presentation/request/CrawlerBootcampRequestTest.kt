package com.ogonggo.adminapi.bootcamp.presentation.request

import com.ogonggo.adminapi.bootcamp.business.CrawlerBootcampCurriculumCommand
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class CrawlerBootcampRequestTest {

    @Test
    fun `보낸 값과 커리큘럼 순서를 그대로 옮긴다`() {
        val command = request(
            curriculums = listOf(
                CrawlerBootcampCurriculumRequest(startWeek = 5, endWeek = 8, subtitle = "스프링"),
                CrawlerBootcampCurriculumRequest(startWeek = 1, endWeek = 4, subtitle = "자바 기초"),
            ),
        ).toCommand()

        assertEquals(OperationType.OFFLINE, command.operationType)
        assertEquals(TuitionType.PAID, command.tuitionType)
        assertEquals(1_200_000L, command.tuitionAmount)
        assertEquals("https://example.com/bootcamps/1", command.sourceUrl)
        assertEquals(
            listOf(
                CrawlerBootcampCurriculumCommand(startWeek = 5, endWeek = 8, subtitle = "스프링"),
                CrawlerBootcampCurriculumCommand(startWeek = 1, endWeek = 4, subtitle = "자바 기초"),
            ),
            command.curriculums,
        )
    }

    @Test
    fun `필드 사이 규칙이 어긋나면 어느 칸이 틀렸는지 알린다`() {
        val cases = mapOf(
            "recruitmentStartAt" to { request(recruitmentStartAt = null) },
            "recruitmentEndAt" to {
                request(recruitmentType = BootcampRecruitmentType.ALWAYS_OPEN)
            },
            "programStartDate" to { request(programStartDate = LocalDate.of(2027, 2, 1)) },
            "applicationUrl" to { request(applicationMethod = ApplicationMethod.EXTERNAL_PAGE, applicationUrl = null) },
            "managerEmail" to { request(managerEmail = " ") },
            "curriculums[0].startWeek" to {
                request(curriculums = listOf(CrawlerBootcampCurriculumRequest(startWeek = 3, endWeek = 2, subtitle = "역순")))
            },
        )

        cases.forEach { (field, request) ->
            val exception = assertThrows<InvalidRequestFieldException> { request().toCommand() }
            assertEquals(field, exception.fieldName)
        }
    }

    @Test
    fun `이메일 지원에는 지원 페이지 주소를 받지 않는다`() {
        val exception = assertThrows<InvalidRequestFieldException> {
            request(applicationMethod = ApplicationMethod.EMAIL, applicationUrl = "https://example.com/apply").toCommand()
        }

        assertEquals("applicationUrl", exception.fieldName)
        assertEquals(null, request(applicationMethod = ApplicationMethod.EMAIL, applicationUrl = null).toCommand().applicationUrl)
    }

    private fun request(
        recruitmentType: BootcampRecruitmentType = BootcampRecruitmentType.PERIOD,
        recruitmentStartAt: LocalDateTime? = LocalDateTime.of(2026, 9, 1, 0, 0),
        programStartDate: LocalDate = LocalDate.of(2026, 10, 6),
        applicationMethod: ApplicationMethod = ApplicationMethod.EXTERNAL_PAGE,
        applicationUrl: String? = "https://example.com/apply",
        managerEmail: String? = null,
        curriculums: List<CrawlerBootcampCurriculumRequest> = emptyList(),
    ) = CrawlerBootcampRequest(
        companyName = "오공고 교육사",
        title = "백엔드 부트캠프",
        programType = "개발",
        operationType = OperationType.OFFLINE,
        recruitmentType = recruitmentType,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = LocalDateTime.of(2026, 9, 30, 23, 59, 59),
        programStartDate = programStartDate,
        programEndDate = LocalDate.of(2027, 1, 30),
        tuitionType = TuitionType.PAID,
        tuitionAmount = 1_200_000,
        representativeImageUrl = "https://example.com/images/bootcamp.png",
        shortDescription = "백엔드 개발자로 성장하는 16주",
        content = "부트캠프 상세 내용",
        applicationMethod = applicationMethod,
        applicationUrl = applicationUrl,
        managerEmail = managerEmail,
        sourceUrl = "https://example.com/bootcamps/1",
        curriculums = curriculums,
    )
}
