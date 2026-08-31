package com.ogonggo.adminapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.implement.BootcampAppender
import com.ogonggo.core.bootcamp.implement.BootcampAppendCommand
import com.ogonggo.core.bootcamp.implement.BootcampManager
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.BootcampUpdateCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AdminBootcampServiceTest {

    private val bootcampReader = Mockito.mock(BootcampReader::class.java)
    private val bootcampAppender = Mockito.mock(BootcampAppender::class.java)
    private val bootcampManager = Mockito.mock(BootcampManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-27T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AdminBootcampService(bootcampReader, bootcampAppender, bootcampManager, clock)

    @Test
    fun `부트캠프를 생성하고 식별자를 반환한다`() {
        val command = appendCommand()
        val bootcamp = Mockito.mock(Bootcamp::class.java)
        Mockito.`when`(bootcamp.id).thenReturn(1L)
        Mockito.`when`(bootcampAppender.append(command)).thenReturn(bootcamp)

        assertEquals(1L, service.create(command))
        Mockito.verify(bootcampAppender).append(command)
    }

    @Test
    fun `부트캠프를 잠금 조회한 뒤 수정한다`() {
        val command = updateCommand()
        val bootcamp = Mockito.mock(Bootcamp::class.java)
        Mockito.`when`(bootcampReader.readForUpdate(1L)).thenReturn(bootcamp)

        service.update(1L, command)

        Mockito.verify(bootcampReader).readForUpdate(1L)
        Mockito.verify(bootcampManager).update(bootcamp, command)
    }

    @Test
    fun `부트캠프를 잠금 조회한 뒤 모집을 시작한다`() {
        val bootcamp = Mockito.mock(Bootcamp::class.java)
        Mockito.`when`(bootcampReader.readForUpdate(1L)).thenReturn(bootcamp)

        service.startRecruitment(1L)

        Mockito.verify(bootcampReader).readForUpdate(1L)
        Mockito.verify(bootcampManager).startRecruitment(bootcamp)
    }

    private fun appendCommand(): BootcampAppendCommand = BootcampAppendCommand(
        companyName = "오공고 교육사",
        title = "백엔드 부트캠프",
        programType = "개발",
        operationType = OperationType.ONLINE,
        recruitmentType = BootcampRecruitmentType.PERIOD,
        recruitmentStartAt = LocalDateTime.of(2026, 8, 1, 0, 0),
        recruitmentEndAt = LocalDateTime.of(2026, 8, 31, 23, 59),
        programStartDate = LocalDate.of(2026, 9, 1),
        programEndDate = LocalDate.of(2026, 12, 1),
        tuitionType = TuitionType.FREE,
        representativeImageUrl = "https://example.com/images/bootcamp.png",
        shortDescription = "백엔드 개발자로 성장하는 12주",
        content = "부트캠프 상세 내용",
        applicationMethod = ApplicationMethod.EXTERNAL_PAGE,
        applicationUrl = "https://example.com/apply",
    )

    private fun updateCommand(): BootcampUpdateCommand = BootcampUpdateCommand(
        companyName = "변경 교육사",
        title = "변경 부트캠프",
        programType = "데이터",
        operationType = OperationType.OFFLINE,
        recruitmentType = BootcampRecruitmentType.PERIOD,
        recruitmentStartAt = LocalDateTime.of(2026, 9, 1, 0, 0),
        recruitmentEndAt = LocalDateTime.of(2026, 9, 30, 23, 59),
        programStartDate = LocalDate.of(2026, 10, 1),
        programEndDate = LocalDate.of(2027, 1, 1),
        tuitionType = TuitionType.PAID,
        representativeImageUrl = "https://example.com/images/updated.png",
        shortDescription = "데이터 분석가로 성장하는 12주",
        content = "변경된 부트캠프 상세 내용",
        applicationMethod = ApplicationMethod.EMAIL,
    )
}
