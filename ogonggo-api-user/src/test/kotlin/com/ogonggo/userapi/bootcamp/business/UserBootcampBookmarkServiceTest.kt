package com.ogonggo.userapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.implement.BootcampBookmarkManager
import com.ogonggo.core.bootcamp.implement.BootcampBookmarkReader
import com.ogonggo.core.bootcamp.implement.BootcampMetricReader
import com.ogonggo.core.bootcamp.implement.BootcampPage
import com.ogonggo.core.bootcamp.implement.BootcampReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class UserBootcampBookmarkServiceTest {

    private val bootcampReader = Mockito.mock(BootcampReader::class.java)
    private val bootcampBookmarkReader = Mockito.mock(BootcampBookmarkReader::class.java)
    private val bootcampBookmarkManager = Mockito.mock(BootcampBookmarkManager::class.java)
    private val bootcampMetricReader = Mockito.mock(BootcampMetricReader::class.java)
    private val eventPublisher = Mockito.mock(ApplicationEventPublisher::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-28T01:00:00Z"), ZONE)
    private val service = UserBootcampBookmarkService(
        bootcampReader,
        bootcampBookmarkReader,
        bootcampBookmarkManager,
        bootcampMetricReader,
        eventPublisher,
        clock,
    )

    @Test
    fun `게시된 부트캠프를 확인한 뒤 북마크를 등록한다`() {
        val bootcamp = Mockito.mock(Bootcamp::class.java)
        Mockito.`when`(bootcampReader.readPublic(BOOTCAMP_ID)).thenReturn(bootcamp)

        service.addBookmark(USER_ID, BOOTCAMP_ID)

        val inOrder = Mockito.inOrder(bootcampReader, bootcampBookmarkManager, eventPublisher)
        inOrder.verify(bootcampReader).readPublic(BOOTCAMP_ID)
        inOrder.verify(bootcampBookmarkManager).append(USER_ID, BOOTCAMP_ID, NOW)
        inOrder.verify(eventPublisher).publishEvent(BootcampBookmarkChangedEvent(BOOTCAMP_ID))
    }

    @Test
    fun `삭제된 부트캠프의 북마크도 멱등하게 해제한다`() {
        val bootcamp = Mockito.mock(Bootcamp::class.java)
        Mockito.`when`(bootcampReader.readIncludingDeleted(BOOTCAMP_ID)).thenReturn(bootcamp)

        service.deleteBookmark(USER_ID, BOOTCAMP_ID)

        Mockito.verify(bootcampBookmarkManager).delete(USER_ID, BOOTCAMP_ID, NOW)
        Mockito.verify(eventPublisher).publishEvent(BootcampBookmarkChangedEvent(BOOTCAMP_ID))
    }

    @Test
    fun `내 북마크 목록은 모든 항목을 북마크 상태로 변환한다`() {
        val bootcamp = createBootcampMock()
        Mockito.`when`(bootcampBookmarkReader.readBookmarkedPublicPage(USER_ID, 0, 10)).thenReturn(
            BootcampPage(listOf(bootcamp), 0, 10, 1, 1, false),
        )

        val result = service.getBookmarks(USER_ID, 0, 10)

        assertEquals(true, result.items.single().bookmarked)
        assertEquals(BOOTCAMP_ID, result.items.single().id)
    }

    private fun createBootcampMock(): Bootcamp = Mockito.mock(Bootcamp::class.java).also { bootcamp ->
        Mockito.`when`(bootcamp.id).thenReturn(BOOTCAMP_ID)
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
        Mockito.`when`(bootcamp.applicationMethod).thenReturn(ApplicationMethod.EXTERNAL_PAGE)
    }

    companion object {
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 28, 10, 0)
        private const val USER_ID = 17L
        private const val BOOTCAMP_ID = 3L
    }
}
