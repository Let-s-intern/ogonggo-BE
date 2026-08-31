package com.ogonggo.userapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.implement.BootcampContentReader
import com.ogonggo.core.bootcamp.implement.BootcampCurriculumData
import com.ogonggo.core.bootcamp.implement.BootcampMetricData
import com.ogonggo.core.bootcamp.implement.BootcampMetricReader
import com.ogonggo.core.bootcamp.implement.BootcampPage
import com.ogonggo.core.bootcamp.implement.BootcampPartnerData
import com.ogonggo.core.bootcamp.implement.BootcampReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate

class UserBootcampServiceTest {

    private val bootcampReader = Mockito.mock(BootcampReader::class.java)
    private val bootcampContentReader = Mockito.mock(BootcampContentReader::class.java)
    private val bootcampMetricReader = Mockito.mock(BootcampMetricReader::class.java)
    private val eventPublisher = Mockito.mock(ApplicationEventPublisher::class.java)
    private val service = UserBootcampService(
        bootcampReader,
        bootcampContentReader,
        bootcampMetricReader,
        eventPublisher,
    )

    @Test
    fun `게시된 부트캠프를 조회해 사용자 결과로 변환한다`() {
        val bootcamp = createBootcampMock()
        Mockito.`when`(bootcampReader.readPublic(1L)).thenReturn(bootcamp)
        Mockito.`when`(bootcampContentReader.readPartners(1L)).thenReturn(
            listOf(BootcampPartnerData("파트너사", 0)),
        )
        Mockito.`when`(bootcampContentReader.readCurriculums(1L)).thenReturn(
            listOf(BootcampCurriculumData(1, 4, "Spring 기초", 0)),
        )
        Mockito.`when`(bootcampMetricReader.read(1L)).thenReturn(
            BootcampMetricData(viewCount = 9, bookmarkCount = 4, commentCount = 0),
        )

        val result = service.getBootcamp(1L)

        assertEquals(1L, result.id)
        assertEquals("오공고 교육사", result.companyName)
        assertEquals("백엔드 부트캠프", result.title)
        assertEquals("파트너사", result.partners.single().name)
        assertEquals("Spring 기초", result.curriculums.single().subtitle)
        assertEquals(9L, result.viewCount)
        assertEquals(4L, result.bookmarkCount)
        assertEquals(0L, result.commentCount)
        Mockito.verify(bootcampReader).readPublic(1L)
    }

    @Test
    fun `상세 조회는 지표를 읽은 뒤 조회 이벤트를 발행한다`() {
        val bootcamp = createBootcampMock()
        Mockito.`when`(bootcampReader.readPublic(1L)).thenReturn(bootcamp)
        Mockito.`when`(bootcampMetricReader.read(1L)).thenReturn(
            BootcampMetricData(viewCount = 1, bookmarkCount = 0, commentCount = 0),
        )

        val result = service.getBootcamp(1L)

        val inOrder = Mockito.inOrder(bootcampMetricReader, eventPublisher)
        inOrder.verify(bootcampMetricReader).read(1L)
        inOrder.verify(eventPublisher).publishEvent(BootcampViewedEvent(1L))
        assertEquals(1L, result.viewCount)
    }

    @Test
    fun `게시된 부트캠프 목록을 페이지 결과로 변환한다`() {
        val bootcamp = createBootcampMock()
        Mockito.`when`(bootcampReader.readPublicPage(0, 20, BootcampSortType.LATEST)).thenReturn(
            BootcampPage(
                bootcamps = listOf(bootcamp),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1,
                hasNext = false,
            ),
        )

        Mockito.`when`(bootcampMetricReader.readAll(listOf(1L))).thenReturn(
            mapOf(1L to BootcampMetricData(viewCount = 7, bookmarkCount = 2, commentCount = 0)),
        )

        val result = service.getBootcamps(0, 20, BootcampSortType.LATEST)

        assertEquals(1, result.items.size)
        assertEquals(1L, result.totalElements)
        assertEquals("백엔드 부트캠프", result.items.single().title)
        assertEquals(7L, result.items.single().viewCount)
        assertEquals(2L, result.items.single().bookmarkCount)
        Mockito.verify(bootcampReader).readPublicPage(0, 20, BootcampSortType.LATEST)
    }

    private fun createBootcampMock(): Bootcamp = Mockito.mock(Bootcamp::class.java).also { bootcamp ->
        Mockito.`when`(bootcamp.id).thenReturn(1L)
        Mockito.`when`(bootcamp.companyName).thenReturn("오공고 교육사")
        Mockito.`when`(bootcamp.title).thenReturn("백엔드 부트캠프")
        Mockito.`when`(bootcamp.programType).thenReturn("개발")
        Mockito.`when`(bootcamp.operationType).thenReturn(OperationType.ONLINE)
        Mockito.`when`(bootcamp.recruitmentType).thenReturn(BootcampRecruitmentType.PERIOD)
        Mockito.`when`(bootcamp.programStartDate).thenReturn(LocalDate.of(2026, 9, 1))
        Mockito.`when`(bootcamp.programEndDate).thenReturn(LocalDate.of(2026, 12, 1))
        Mockito.`when`(bootcamp.tuitionType).thenReturn(TuitionType.FREE)
        Mockito.`when`(bootcamp.representativeImageUrl).thenReturn("https://example.com/image.png")
        Mockito.`when`(bootcamp.shortDescription).thenReturn("백엔드 개발자로 성장하는 12주")
        Mockito.`when`(bootcamp.status).thenReturn(BootcampStatus.RECRUITING)
        Mockito.`when`(bootcamp.content).thenReturn("부트캠프 상세 내용")
        Mockito.`when`(bootcamp.applicationMethod).thenReturn(ApplicationMethod.EXTERNAL_PAGE)
    }
}
