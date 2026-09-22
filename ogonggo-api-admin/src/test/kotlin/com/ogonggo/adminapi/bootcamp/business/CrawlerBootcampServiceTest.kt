package com.ogonggo.adminapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.error.BootcampErrorCode
import com.ogonggo.core.bootcamp.implement.BootcampAppender
import com.ogonggo.core.bootcamp.implement.BootcampContentReader
import com.ogonggo.core.bootcamp.implement.BootcampManager
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampCurriculumDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampPartnerDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampUpdateDto
import com.ogonggo.core.error.ConflictException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CrawlerBootcampServiceTest {

    private val bootcampReader = Mockito.mock(BootcampReader::class.java)
    private val bootcampContentReader = Mockito.mock(BootcampContentReader::class.java)
    private val savedBootcamp = Mockito.mock(Bootcamp::class.java).also {
        Mockito.`when`(it.id).thenReturn(BOOTCAMP_ID)
    }

    /**
     * 코틀린에서는 `any()`와 `capture()`가 null을 돌려줘 non-null 파라미터에 넘길 수 없다.
     * 인자 매처 대신 호출을 그대로 받아 기록한다.
     */
    private var appendedCommand: BootcampAppendDto? = null
    private val bootcampAppender = Mockito.mock(BootcampAppender::class.java, Answer { invocation ->
        appendedCommand = invocation.arguments[0] as BootcampAppendDto
        savedBootcamp
    })

    private var updatedCommand: BootcampUpdateDto? = null
    private var deletedAt: LocalDateTime? = null
    private var closedAt: LocalDateTime? = null
    private val managerCalls = mutableListOf<String>()
    private val bootcampManager = Mockito.mock(BootcampManager::class.java, Answer { invocation ->
        managerCalls += invocation.method.name
        when (invocation.method.name) {
            "update" -> updatedCommand = invocation.arguments[1] as BootcampUpdateDto
            "delete" -> deletedAt = invocation.arguments[1] as LocalDateTime
            "close" -> closedAt = invocation.arguments[1] as LocalDateTime
        }
        null
    })

    private val clock = Clock.fixed(Instant.parse("2026-09-22T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service =
        CrawlerBootcampService(bootcampReader, bootcampAppender, bootcampManager, bootcampContentReader, clock)

    @Test
    fun `수집한 부트캠프를 검수 없이 모집 중으로 게시하고 커리큘럼은 보낸 순서대로 저장한다`() {
        Mockito.`when`(bootcampReader.existsBySourceUrl(SOURCE_URL)).thenReturn(false)

        val bootcampId = service.register(command())

        assertEquals(BOOTCAMP_ID, bootcampId)
        val appended = checkNotNull(appendedCommand)
        assertNull(appended.ownerUserId)
        assertEquals(BootcampStatus.RECRUITING, appended.status)
        assertEquals(BootcampPublicationStatus.PUBLISHED, appended.publicationStatus)
        assertEquals(SOURCE_URL, appended.sourceUrl)
        assertEquals("오공고 교육사", appended.companyName)
        assertEquals(TuitionType.GOVERNMENT_FUNDED, appended.tuitionType)
        assertEquals("edu@example.com", appended.managerEmail)
        assertEquals(
            listOf(
                BootcampCurriculumDto.Request(startWeek = 1, endWeek = 4, subtitle = "자바 기초", displayOrder = 0),
                BootcampCurriculumDto.Request(startWeek = 5, endWeek = 8, subtitle = "스프링", displayOrder = 1),
            ),
            appended.curriculums,
        )
        // 모집 상태를 보내지 않으면 모집 중 그대로 두고 따로 바꾸지 않는다.
        assertEquals(emptyList<String>(), managerCalls)
    }

    @Test
    fun `모집 마감으로 보내면 모집 중으로 저장한 뒤 현재 시각으로 마감하고 게시한다`() {
        Mockito.`when`(bootcampReader.existsBySourceUrl(SOURCE_URL)).thenReturn(false)

        val bootcampId = service.register(command(status = BootcampStatus.CLOSED))

        assertEquals(BOOTCAMP_ID, bootcampId)
        val appended = checkNotNull(appendedCommand)
        assertEquals(BootcampStatus.RECRUITING, appended.status)
        assertNull(appended.closedAt)
        assertEquals(BootcampPublicationStatus.PUBLISHED, appended.publicationStatus)
        assertEquals(listOf("close"), managerCalls)
        assertEquals(NOW, closedAt)
    }

    @Test
    fun `모집 중으로 보내면 따로 상태를 바꾸지 않는다`() {
        Mockito.`when`(bootcampReader.existsBySourceUrl(SOURCE_URL)).thenReturn(false)

        service.register(command(status = BootcampStatus.RECRUITING))

        assertEquals(BootcampStatus.RECRUITING, checkNotNull(appendedCommand).status)
        assertEquals(emptyList<String>(), managerCalls)
    }

    @Test
    fun `이미 등록된 원문은 저장하지 않고 충돌로 알린다`() {
        Mockito.`when`(bootcampReader.existsBySourceUrl(SOURCE_URL)).thenReturn(true)

        val exception = assertThrows<ConflictException> { service.register(command()) }

        assertEquals(BootcampErrorCode.BOOTCAMP_ALREADY_EXISTS, exception.errorCode)
        assertNull(appendedCommand)
    }

    @Test
    fun `교체는 값과 커리큘럼을 바꾸고 크롤러가 보내지 않는 공개 기간과 파트너사는 그대로 둔다`() {
        val bootcamp = stubCrawledBootcamp()
        val publicationEndAt = LocalDateTime.of(2026, 12, 31, 23, 59)
        Mockito.`when`(bootcamp.publicationEndAt).thenReturn(publicationEndAt)
        Mockito.`when`(bootcampContentReader.readPartners(BOOTCAMP_ID))
            .thenReturn(listOf(BootcampPartnerDto.Response(name = "렛츠커리어", displayOrder = 2)))

        service.replace(BOOTCAMP_ID, command(title = "바뀐 과정", curriculums = listOf(curriculum(1, 2, "새 과정"))))

        val updated = checkNotNull(updatedCommand)
        assertEquals("바뀐 과정", updated.title)
        assertEquals(listOf(BootcampCurriculumDto.Request(1, 2, "새 과정", 0)), updated.curriculums)
        assertEquals(publicationEndAt, updated.publicationEndAt)
        assertNull(updated.publicationStartAt)
        assertEquals(listOf(BootcampPartnerDto.Request(partnerName = "렛츠커리어", displayOrder = 2)), updated.partners)
        // 게시·모집 상태를 바꾸는 호출은 없다.
        assertEquals(listOf("update"), managerCalls)
    }

    @Test
    fun `교체에 모집 마감을 보내면 모집 중이던 부트캠프를 현재 시각으로 마감한다`() {
        val bootcamp = stubCrawledBootcamp()
        Mockito.`when`(bootcamp.status).thenReturn(BootcampStatus.RECRUITING)

        service.replace(BOOTCAMP_ID, command(status = BootcampStatus.CLOSED))

        assertEquals(listOf("update", "close"), managerCalls)
        assertEquals(NOW, closedAt)
    }

    @Test
    fun `교체에 모집 중을 보내면 마감된 부트캠프를 다시 모집 중으로 바꾼다`() {
        val bootcamp = stubCrawledBootcamp()
        Mockito.`when`(bootcamp.status).thenReturn(BootcampStatus.CLOSED)

        service.replace(BOOTCAMP_ID, command(status = BootcampStatus.RECRUITING))

        assertEquals(listOf("update", "startRecruitment"), managerCalls)
    }

    @Test
    fun `교체에 지금과 같은 모집 상태를 보내면 상태를 바꾸지 않는다`() {
        listOf(BootcampStatus.RECRUITING, BootcampStatus.CLOSED).forEach { status ->
            managerCalls.clear()
            val bootcamp = stubCrawledBootcamp()
            Mockito.`when`(bootcamp.status).thenReturn(status)

            service.replace(BOOTCAMP_ID, command(status = status))

            assertEquals(listOf("update"), managerCalls, "$status")
        }
        assertNull(closedAt)
    }

    @Test
    fun `다른 부트캠프가 쓰는 원문 URL로 바꾸려 하면 충돌로 알린다`() {
        stubCrawledBootcamp()
        val otherUrl = "https://example.com/bootcamps/2"
        Mockito.`when`(bootcampReader.existsBySourceUrl(otherUrl)).thenReturn(true)

        val exception = assertThrows<ConflictException> { service.replace(BOOTCAMP_ID, command(sourceUrl = otherUrl)) }

        assertEquals(BootcampErrorCode.BOOTCAMP_ALREADY_EXISTS, exception.errorCode)
        assertEquals(emptyList<String>(), managerCalls)
    }

    @Test
    fun `수집 부트캠프를 현재 시각으로 삭제한다`() {
        Mockito.`when`(bootcampReader.readCrawledForDelete(BOOTCAMP_ID)).thenReturn(Mockito.mock(Bootcamp::class.java))

        service.delete(BOOTCAMP_ID)

        assertEquals(listOf("delete"), managerCalls)
        assertEquals(NOW, deletedAt)
    }

    @Test
    fun `원문 URL로 수집 부트캠프의 식별자를 찾는다`() {
        Mockito.`when`(bootcampReader.readCrawledBySourceUrl(SOURCE_URL)).thenReturn(savedBootcamp)

        assertEquals(BOOTCAMP_ID, service.getBootcampId(SOURCE_URL))
    }

    private fun stubCrawledBootcamp(): Bootcamp {
        val bootcamp = Mockito.mock(Bootcamp::class.java)
        Mockito.`when`(bootcamp.sourceUrl).thenReturn(SOURCE_URL)
        Mockito.`when`(bootcampReader.readCrawledForUpdate(BOOTCAMP_ID)).thenReturn(bootcamp)
        return bootcamp
    }

    private fun curriculum(startWeek: Int, endWeek: Int, subtitle: String) =
        CrawlerBootcampCurriculumCommand(startWeek = startWeek, endWeek = endWeek, subtitle = subtitle)

    private fun command(
        title: String = "백엔드 부트캠프",
        sourceUrl: String = SOURCE_URL,
        status: BootcampStatus? = null,
        curriculums: List<CrawlerBootcampCurriculumCommand> = listOf(curriculum(1, 4, "자바 기초"), curriculum(5, 8, "스프링")),
    ): CrawlerBootcampCommand = CrawlerBootcampCommand(
        companyName = "오공고 교육사",
        title = title,
        programType = "개발",
        operationType = OperationType.HYBRID,
        recruitmentType = BootcampRecruitmentType.PERIOD,
        recruitmentStartAt = LocalDateTime.of(2026, 9, 1, 0, 0),
        recruitmentEndAt = LocalDateTime.of(2026, 9, 30, 23, 59, 59),
        programStartDate = LocalDate.of(2026, 10, 6),
        programEndDate = LocalDate.of(2027, 1, 30),
        capacity = 30,
        tuitionType = TuitionType.GOVERNMENT_FUNDED,
        tuitionAmount = null,
        representativeImageUrl = "https://example.com/images/bootcamp.png",
        shortDescription = "백엔드 개발자로 성장하는 16주",
        content = "부트캠프 상세 내용",
        eligibilityAndSelectionProcess = null,
        applicationMethod = ApplicationMethod.EXTERNAL_PAGE,
        applicationUrl = "https://example.com/apply",
        managerEmail = "edu@example.com",
        inquiryUrl = null,
        sourceUrl = sourceUrl,
        status = status,
        curriculums = curriculums,
    )

    companion object {
        private const val BOOTCAMP_ID = 11L
        private const val SOURCE_URL = "https://example.com/bootcamps/1"
        private val NOW = LocalDateTime.of(2026, 9, 22, 12, 0)
    }
}
