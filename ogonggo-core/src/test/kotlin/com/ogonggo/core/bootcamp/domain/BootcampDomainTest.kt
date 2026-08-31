package com.ogonggo.core.bootcamp.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BootcampDomainTest {

    @Test
    fun `부트캠프 정보를 수정한다`() {
        val bootcamp = createBootcamp()
        val recruitmentStartAt = LocalDateTime.of(2026, 9, 1, 0, 0)
        val recruitmentEndAt = recruitmentStartAt.plusDays(14)
        val programStartDate = LocalDate.of(2026, 10, 1)
        val programEndDate = programStartDate.plusMonths(3)
        val publicationStartAt = LocalDateTime.of(2026, 8, 20, 0, 0)
        val publicationEndAt = publicationStartAt.plusMonths(1)

        bootcamp.update(
            companyName = "변경 교육사",
            title = "데이터 분석 부트캠프",
            programType = "데이터",
            operationType = OperationType.OFFLINE,
            recruitmentType = BootcampRecruitmentType.PERIOD,
            recruitmentStartAt = recruitmentStartAt,
            recruitmentEndAt = recruitmentEndAt,
            programStartDate = programStartDate,
            programEndDate = programEndDate,
            capacity = 30,
            tuitionType = TuitionType.PAID,
            tuitionAmount = 1_000_000,
            representativeImageUrl = "https://example.com/images/bootcamp-2.png",
            shortDescription = "데이터 분석가로 성장하는 12주",
            content = "변경된 부트캠프 상세 내용",
            eligibilityAndSelectionProcess = "서류 검토 후 인터뷰를 진행합니다.",
            applicationMethod = ApplicationMethod.EMAIL,
            applicationUrl = null,
            managerEmail = "manager@example.com",
            inquiryUrl = "https://example.com/inquiry",
            publicationStartAt = publicationStartAt,
            publicationEndAt = publicationEndAt,
            sourceUrl = "https://example.com/bootcamps/2",
        )

        assertEquals("변경 교육사", bootcamp.companyName)
        assertEquals("데이터 분석 부트캠프", bootcamp.title)
        assertEquals(OperationType.OFFLINE, bootcamp.operationType)
        assertEquals(30, bootcamp.capacity)
        assertEquals(TuitionType.PAID, bootcamp.tuitionType)
        assertEquals(1_000_000, bootcamp.tuitionAmount)
        assertEquals("데이터 분석가로 성장하는 12주", bootcamp.shortDescription)
        assertEquals(ApplicationMethod.EMAIL, bootcamp.applicationMethod)
        assertEquals(publicationStartAt, bootcamp.publicationStartAt)
    }

    @Test
    fun `필수값과 숫자 및 기간을 검증한다`() {
        assertThrows(IllegalArgumentException::class.java) { createBootcamp(programType = " ") }
        assertThrows(IllegalArgumentException::class.java) { createBootcamp(representativeImageUrl = " ") }
        assertThrows(IllegalArgumentException::class.java) { createBootcamp(shortDescription = " ") }
        assertThrows(IllegalArgumentException::class.java) { createBootcamp(capacity = -1) }
        assertThrows(IllegalArgumentException::class.java) { createBootcamp(tuitionAmount = -1) }
        assertThrows(IllegalArgumentException::class.java) {
            createBootcamp(
                recruitmentStartAt = LocalDateTime.of(2026, 9, 2, 0, 0),
                recruitmentEndAt = LocalDateTime.of(2026, 9, 1, 0, 0),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            createBootcamp(
                publicationStartAt = LocalDateTime.of(2026, 9, 2, 0, 0),
                publicationEndAt = LocalDateTime.of(2026, 9, 1, 0, 0),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            createBootcamp(
                programStartDate = LocalDate.of(2026, 12, 1),
                programEndDate = LocalDate.of(2026, 11, 1),
            )
        }
    }

    @Test
    fun `기간 모집은 모집 시작과 마감 일시가 필수다`() {
        assertThrows(IllegalArgumentException::class.java) { createBootcamp(recruitmentStartAt = null) }
        assertThrows(IllegalArgumentException::class.java) { createBootcamp(recruitmentEndAt = null) }

        createBootcamp(
            recruitmentType = BootcampRecruitmentType.ALWAYS_OPEN,
            recruitmentStartAt = null,
            recruitmentEndAt = null,
        )
    }

    @Test
    fun `지원 방법에 따라 외부 지원 링크를 검증한다`() {
        assertThrows(IllegalArgumentException::class.java) { createBootcamp(applicationUrl = null) }
        assertThrows(IllegalArgumentException::class.java) {
            createBootcamp(
                applicationMethod = ApplicationMethod.EMAIL,
                applicationUrl = "https://example.com/apply",
            )
        }

        createBootcamp(
            applicationMethod = ApplicationMethod.EMAIL,
            applicationUrl = null,
            managerEmail = null,
        )
    }

    @Test
    fun `모집 시작 마감과 재모집을 처리한다`() {
        val bootcamp = createBootcamp()
        val firstClosedAt = LocalDateTime.of(2026, 9, 1, 0, 0)

        bootcamp.startRecruitment()
        assertEquals(BootcampStatus.RECRUITING, bootcamp.status)

        bootcamp.close(firstClosedAt)
        bootcamp.close(firstClosedAt.plusDays(1))
        assertEquals(BootcampStatus.CLOSED, bootcamp.status)
        assertEquals(firstClosedAt, bootcamp.closedAt)

        bootcamp.startRecruitment()
        assertEquals(BootcampStatus.RECRUITING, bootcamp.status)
        assertEquals(null, bootcamp.closedAt)
    }

    @Test
    fun `삭제는 멱등하며 삭제 후 변경을 차단한다`() {
        val bootcamp = createBootcamp()
        val firstDeletedAt = LocalDateTime.of(2026, 9, 1, 0, 0)

        bootcamp.delete(firstDeletedAt)
        bootcamp.delete(firstDeletedAt.plusDays(1))

        assertEquals(firstDeletedAt, bootcamp.deletedAt)
        assertThrows(IllegalStateException::class.java) { bootcamp.startRecruitment() }
    }

    private fun createBootcamp(
        programType: String = "개발",
        recruitmentType: BootcampRecruitmentType = BootcampRecruitmentType.PERIOD,
        recruitmentStartAt: LocalDateTime? = LocalDateTime.of(2026, 8, 1, 0, 0),
        recruitmentEndAt: LocalDateTime? = LocalDateTime.of(2026, 8, 31, 23, 59),
        programStartDate: LocalDate = LocalDate.of(2026, 9, 1),
        programEndDate: LocalDate = LocalDate.of(2026, 12, 1),
        capacity: Int? = 50,
        tuitionAmount: Long? = 0,
        representativeImageUrl: String = "https://example.com/images/bootcamp.png",
        shortDescription: String = "백엔드 개발자로 성장하는 12주",
        applicationMethod: ApplicationMethod = ApplicationMethod.EXTERNAL_PAGE,
        applicationUrl: String? = "https://example.com/apply",
        managerEmail: String? = null,
        publicationStartAt: LocalDateTime? = null,
        publicationEndAt: LocalDateTime? = null,
    ): Bootcamp = Bootcamp(
        companyName = "오공고 교육사",
        title = "백엔드 부트캠프",
        programType = programType,
        operationType = OperationType.ONLINE,
        recruitmentType = recruitmentType,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = recruitmentEndAt,
        programStartDate = programStartDate,
        programEndDate = programEndDate,
        capacity = capacity,
        tuitionType = TuitionType.FREE,
        tuitionAmount = tuitionAmount,
        representativeImageUrl = representativeImageUrl,
        shortDescription = shortDescription,
        content = "부트캠프 상세 내용",
        applicationMethod = applicationMethod,
        applicationUrl = applicationUrl,
        managerEmail = managerEmail,
        publicationStartAt = publicationStartAt,
        publicationEndAt = publicationEndAt,
        sourceUrl = "https://example.com/bootcamps/1",
    )
}
