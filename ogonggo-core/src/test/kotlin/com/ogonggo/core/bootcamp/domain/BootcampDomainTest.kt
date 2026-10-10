package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.contentreview.domain.ContentSource
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
            operationType = BootcampOperationType.OFFLINE,
            recruitmentType = BootcampRecruitmentType.PERIOD,
            recruitmentStartAt = recruitmentStartAt,
            recruitmentEndAt = recruitmentEndAt,
            programStartDate = programStartDate,
            programEndDate = programEndDate,
            capacity = 30,
            tuitionType = BootcampTuitionType.PAID,
            tuitionAmount = 1_000_000,
            representativeImageUrl = "https://example.com/images/bootcamp-2.png",
            shortDescription = "데이터 분석가로 성장하는 12주",
            content = "변경된 부트캠프 상세 내용",
            eligibilityAndSelectionProcess = "서류 검토 후 인터뷰를 진행합니다.",
            logoUrl = "https://example.com/images/logo.png",
            instructorInfo = "현업 데이터 분석가가 강의합니다.",
            programFeatures = "매주 실데이터 프로젝트를 진행합니다.",
            completionRequirements = "출석률 80% 이상",
            applicationMethod = BootcampApplicationMethod.EMAIL,
            applicationUrl = null,
            managerEmail = "manager@example.com",
            inquiryUrl = "https://example.com/inquiry",
            publicationStartAt = publicationStartAt,
            publicationEndAt = publicationEndAt,
            sourceUrl = "https://example.com/bootcamps/2",
        )

        assertEquals("변경 교육사", bootcamp.companyName)
        assertEquals("데이터 분석 부트캠프", bootcamp.title)
        assertEquals(BootcampOperationType.OFFLINE, bootcamp.operationType)
        assertEquals(30, bootcamp.capacity)
        assertEquals(BootcampTuitionType.PAID, bootcamp.tuitionType)
        assertEquals(1_000_000, bootcamp.tuitionAmount)
        assertEquals("데이터 분석가로 성장하는 12주", bootcamp.shortDescription)
        assertEquals(BootcampApplicationMethod.EMAIL, bootcamp.applicationMethod)
        assertEquals("https://example.com/images/logo.png", bootcamp.logoUrl)
        assertEquals("현업 데이터 분석가가 강의합니다.", bootcamp.instructorInfo)
        assertEquals("매주 실데이터 프로젝트를 진행합니다.", bootcamp.programFeatures)
        assertEquals("출석률 80% 이상", bootcamp.completionRequirements)
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
                applicationMethod = BootcampApplicationMethod.EMAIL,
                applicationUrl = "https://example.com/apply",
            )
        }

        createBootcamp(
            applicationMethod = BootcampApplicationMethod.EMAIL,
            applicationUrl = null,
            managerEmail = null,
        )
    }

    @Test
    fun `모집 시작 마감과 재모집을 처리한다`() {
        val bootcamp = createBootcamp()
        val firstClosedAt = LocalDateTime.of(2026, 9, 1, 0, 0)

        bootcamp.startRecruitment()
        assertEquals(BootcampRecruitmentStatus.RECRUITING, bootcamp.status)

        bootcamp.close(firstClosedAt)
        bootcamp.close(firstClosedAt.plusDays(1))
        assertEquals(BootcampRecruitmentStatus.CLOSED, bootcamp.status)
        assertEquals(firstClosedAt, bootcamp.closedAt)

        bootcamp.startRecruitment()
        assertEquals(BootcampRecruitmentStatus.RECRUITING, bootcamp.status)
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

    @Test
    fun `부트캠프도 고용24 수집만 외부 식별값을 가진다`() {
        assertEquals(ContentSource.CRAWLER, createBootcamp().source)
        assertEquals("C1-1", createBootcamp(source = ContentSource.WORK24, externalId = "C1-1").externalId)

        assertThrows(IllegalArgumentException::class.java) { createBootcamp(source = ContentSource.WORK24) }
        assertThrows(IllegalArgumentException::class.java) { createBootcamp(source = ContentSource.COMPANY) }
        assertThrows(IllegalArgumentException::class.java) { createBootcamp(externalId = "C1-1") }
    }

    @Test
    fun `대표 이미지는 비워 둘 수 있지만 공백일 수는 없다`() {
        assertEquals(null, createBootcamp(representativeImageUrl = null).representativeImageUrl)
        assertThrows(IllegalArgumentException::class.java) { createBootcamp(representativeImageUrl = " ") }
    }

    private fun createBootcamp(
        source: ContentSource = ContentSource.CRAWLER,
        externalId: String? = null,
        programType: String = "개발",
        recruitmentType: BootcampRecruitmentType = BootcampRecruitmentType.PERIOD,
        recruitmentStartAt: LocalDateTime? = LocalDateTime.of(2026, 8, 1, 0, 0),
        recruitmentEndAt: LocalDateTime? = LocalDateTime.of(2026, 8, 31, 23, 59),
        programStartDate: LocalDate = LocalDate.of(2026, 9, 1),
        programEndDate: LocalDate = LocalDate.of(2026, 12, 1),
        capacity: Int? = 50,
        tuitionAmount: Long? = 0,
        representativeImageUrl: String? = "https://example.com/images/bootcamp.png",
        shortDescription: String = "백엔드 개발자로 성장하는 12주",
        applicationMethod: BootcampApplicationMethod = BootcampApplicationMethod.EXTERNAL_PAGE,
        applicationUrl: String? = "https://example.com/apply",
        managerEmail: String? = null,
        publicationStartAt: LocalDateTime? = null,
        publicationEndAt: LocalDateTime? = null,
    ): Bootcamp = Bootcamp(
        companyName = "오공고 교육사",
        title = "백엔드 부트캠프",
        programType = programType,
        operationType = BootcampOperationType.ONLINE,
        recruitmentType = recruitmentType,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = recruitmentEndAt,
        programStartDate = programStartDate,
        programEndDate = programEndDate,
        capacity = capacity,
        tuitionType = BootcampTuitionType.FREE,
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
        source = source,
        externalId = externalId,
    )
}
