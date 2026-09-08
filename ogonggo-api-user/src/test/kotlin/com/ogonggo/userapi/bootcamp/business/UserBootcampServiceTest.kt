package com.ogonggo.userapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.implement.BootcampApplicationUrlClickAppender
import com.ogonggo.core.bootcamp.implement.BootcampBookmarkReader
import com.ogonggo.core.bootcamp.implement.BootcampContentReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampCurriculumDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampMetricDto
import com.ogonggo.core.bootcamp.implement.BootcampMetricReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampPageDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampPartnerDto
import com.ogonggo.core.bootcamp.implement.BootcampReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate

class UserBootcampServiceTest {

    private val bootcampReader = Mockito.mock(BootcampReader::class.java)
    private val bootcampBookmarkReader = Mockito.mock(BootcampBookmarkReader::class.java)
    private val bootcampContentReader = Mockito.mock(BootcampContentReader::class.java)
    private val bootcampApplicationUrlClickAppender =
        Mockito.mock(BootcampApplicationUrlClickAppender::class.java)
    private val bootcampMetricReader = Mockito.mock(BootcampMetricReader::class.java)
    private val eventPublisher = Mockito.mock(ApplicationEventPublisher::class.java)
    private val service = UserBootcampService(
        bootcampReader,
        bootcampBookmarkReader,
        bootcampContentReader,
        bootcampApplicationUrlClickAppender,
        bootcampMetricReader,
        eventPublisher,
    )

    @Test
    fun `게시된 부트캠프를 조회해 사용자 결과로 변환한다`() {
        val bootcamp = createBootcampMock()
        Mockito.`when`(bootcampReader.readPublic(1L)).thenReturn(bootcamp)
        Mockito.`when`(bootcampContentReader.readPartners(1L)).thenReturn(
            listOf(BootcampPartnerDto.Response("파트너사", 0)),
        )
        Mockito.`when`(bootcampContentReader.readCurriculums(1L)).thenReturn(
            listOf(BootcampCurriculumDto.Response(1, 4, "Spring 기초", 0)),
        )
        Mockito.`when`(bootcampMetricReader.read(1L)).thenReturn(
            BootcampMetricDto(viewCount = 9, bookmarkCount = 4, commentCount = 0),
        )

        Mockito.`when`(bootcampBookmarkReader.readBookmarkedBootcampIds(USER_ID, listOf(1L)))
            .thenReturn(setOf(1L))

        val result = service.getBootcamp(USER_ID, 1L)

        assertEquals(1L, result.id)
        assertEquals(true, result.bookmarked)
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
            BootcampMetricDto(viewCount = 1, bookmarkCount = 0, commentCount = 0),
        )

        val result = service.getBootcamp(USER_ID, 1L)

        val inOrder = Mockito.inOrder(bootcampMetricReader, eventPublisher)
        inOrder.verify(bootcampMetricReader).read(1L)
        inOrder.verify(eventPublisher).publishEvent(BootcampViewedEvent(1L))
        assertEquals(1L, result.viewCount)
    }

    @Test
    fun `게시된 부트캠프 목록을 페이지 결과로 변환한다`() {
        val bootcamp = createBootcampMock()
        Mockito.`when`(
            bootcampReader.readPublicPage(BootcampSearchCondition.NONE, BootcampSortType.LATEST, 0, 20),
        ).thenReturn(
            BootcampPageDto(
                bootcamps = listOf(bootcamp),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1,
                hasNext = false,
            ),
        )

        Mockito.`when`(bootcampMetricReader.readAll(listOf(1L))).thenReturn(
            mapOf(1L to BootcampMetricDto(viewCount = 7, bookmarkCount = 2, commentCount = 0)),
        )

        Mockito.`when`(bootcampBookmarkReader.readBookmarkedBootcampIds(USER_ID, listOf(1L)))
            .thenReturn(setOf(1L))

        val result = service.getBootcamps(USER_ID, BootcampSearchCondition.NONE, BootcampSortType.LATEST, 0, 20)

        assertEquals(1, result.items.size)
        assertEquals(true, result.items.single().bookmarked)
        assertEquals(1L, result.totalElements)
        assertEquals("백엔드 부트캠프", result.items.single().title)
        assertEquals(7L, result.items.single().viewCount)
        assertEquals(2L, result.items.single().bookmarkCount)
        Mockito.verify(bootcampReader)
            .readPublicPage(BootcampSearchCondition.NONE, BootcampSortType.LATEST, 0, 20)
    }

    @Test
    fun `비로그인 조회는 북마크 저장소를 읽지 않고 북마크를 false로 채운다`() {
        val bootcamp = createBootcampMock()
        Mockito.`when`(bootcampReader.readPublic(1L)).thenReturn(bootcamp)
        Mockito.`when`(bootcampMetricReader.read(1L)).thenReturn(BootcampMetricDto.EMPTY)

        val result = service.getBootcamp(null, 1L)

        assertEquals(false, result.bookmarked)
        Mockito.verifyNoInteractions(bootcampBookmarkReader)
    }

    @Test
    fun `게시된 부트캠프를 확인한 뒤 지원 페이지 이동을 기록한다`() {
        val bootcamp = Mockito.mock(Bootcamp::class.java)
        Mockito.`when`(bootcampReader.readPublic(1L)).thenReturn(bootcamp)

        service.recordApplicationUrlClick(USER_ID, 1L)

        val inOrder = Mockito.inOrder(bootcampReader, bootcampApplicationUrlClickAppender)
        inOrder.verify(bootcampReader).readPublic(1L)
        inOrder.verify(bootcampApplicationUrlClickAppender).append(USER_ID, 1L)
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

    companion object {
        private const val USER_ID = 17L
    }
}
