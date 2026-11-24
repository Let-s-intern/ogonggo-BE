package com.ogonggo.core.contentevent.implement

import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentevent.implement.dto.ContentEventAppendDto
import com.ogonggo.core.jpa.CoreJpaConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(ContentEventAppender::class, ContentEventReader::class)
internal class ContentEventReaderTest @Autowired constructor(
    private val contentEventAppender: ContentEventAppender,
    private val contentEventReader: ContentEventReader,
) {

    @Test
    fun `같은 방문자가 같은 대상을 30분 안에 다시 열면 조회수에 넣지 않고 정확히 30분이 지나면 넣는다`() {
        // given
        contentEventAppender.appendAll(listOf(view()), FIRST)

        // when & then
        assertEquals(emptyList<ContentEventAppendDto>(), contentEventReader.filterCountableViews(listOf(view()), FIRST.plusMinutes(29).plusSeconds(59)))
        assertEquals(listOf(view()), contentEventReader.filterCountableViews(listOf(view()), FIRST.plusMinutes(30)))
        assertEquals(listOf(view()), contentEventReader.filterCountableViews(listOf(view()), FIRST.plusMinutes(31)))
    }

    @Test
    fun `직전 조회가 조회수에 들어가지 않은 조회여도 그 시각이 기준이 된다`() {
        // given: 0분 조회, 20분 재조회(중복), 40분에 다시 연다
        contentEventAppender.appendAll(listOf(view()), FIRST)
        contentEventAppender.appendAll(listOf(view()), FIRST.plusMinutes(20))

        // when & then: 0분 기준으로는 30분이 지났지만 직전 조회(20분)로부터는 20분이다
        assertEquals(emptyList<ContentEventAppendDto>(), contentEventReader.filterCountableViews(listOf(view()), FIRST.plusMinutes(40)))
        assertEquals(listOf(view()), contentEventReader.filterCountableViews(listOf(view()), FIRST.plusMinutes(50)))
    }

    @Test
    fun `다른 방문자나 다른 대상이나 다른 종류의 이벤트는 서로 중복이 아니다`() {
        // given
        contentEventAppender.appendAll(listOf(view(), view().copy(eventType = ContentEventType.CTA_CLICK)), FIRST)
        val otherVisitor = view().copy(visitorId = OTHER_VISITOR_ID)
        val otherTarget = view().copy(targetId = 16L)
        val otherType = view().copy(targetType = ContentEventTargetType.CONCERN)
        val now = FIRST.plusMinutes(5)

        // when
        val countable = contentEventReader.filterCountableViews(listOf(otherVisitor, otherTarget, otherType, view()), now)

        // then
        assertEquals(listOf(otherVisitor, otherTarget, otherType), countable)
    }

    @Test
    fun `조회가 아닌 이벤트와 대상 없는 이벤트는 조회수에 넣지 않는다`() {
        val click = view().copy(eventType = ContentEventType.CTA_CLICK)
        val scroll = view().copy(eventType = ContentEventType.SCROLL, scrollPercent = 50, durationMs = 1000L)
        val banner = view().copy(eventType = ContentEventType.IMPRESSION, targetType = null, targetId = null, exposureId = 3L)

        assertEquals(emptyList<ContentEventAppendDto>(), contentEventReader.filterCountableViews(listOf(click, scroll, banner), FIRST))
    }

    @Test
    fun `한 요청 안에서 같은 대상의 둘째 조회부터는 넣지 않는다`() {
        val job = view()
        val concern = view().copy(targetType = ContentEventTargetType.CONCERN)

        assertEquals(listOf(job, concern), contentEventReader.filterCountableViews(listOf(job, concern, job, concern), FIRST))
    }

    @Test
    fun `회원은 회원 식별자로 남은 조회와 방문자 식별자로 남은 조회를 같은 사람으로 본다`() {
        // given: 로그인 전 익명 조회(방문자 식별자만), 다른 기기에서 회원으로 연 조회(회원 식별자만)
        contentEventAppender.appendAll(listOf(view()), FIRST)
        contentEventAppender.appendAll(listOf(view().copy(visitorId = OTHER_VISITOR_ID, userId = MEMBER_ID, targetId = 16L)), FIRST)
        val now = FIRST.plusMinutes(10)

        // when
        val loginAfterAnonymousView = view().copy(userId = MEMBER_ID)
        val otherDeviceView = view().copy(userId = MEMBER_ID, targetId = 16L)
        val otherMemberView = view().copy(userId = MEMBER_ID + 1, visitorId = OTHER_VISITOR_ID)

        // then
        assertEquals(emptyList<ContentEventAppendDto>(), contentEventReader.filterCountableViews(listOf(loginAfterAnonymousView, otherDeviceView), now))
        assertEquals(listOf(otherMemberView), contentEventReader.filterCountableViews(listOf(otherMemberView), now))
    }

    @Test
    fun `익명 방문자는 같은 방문자 식별자의 회원 조회도 같은 사람으로 본다`() {
        contentEventAppender.appendAll(listOf(view().copy(userId = MEMBER_ID)), FIRST)

        assertEquals(emptyList<ContentEventAppendDto>(), contentEventReader.filterCountableViews(listOf(view()), FIRST.plusMinutes(10)))
    }

    private fun view() = ContentEventAppendDto(
        eventType = ContentEventType.VIEW,
        targetType = ContentEventTargetType.JOB,
        targetId = 15L,
        exposureId = null,
        userId = null,
        visitorId = VISITOR_ID,
        source = ContentEventSource.LIST,
    )

    private companion object {
        const val VISITOR_ID = "5f0c2e9e-1b7a-4c1e-9a43-0d2f3b6c7a11"
        const val OTHER_VISITOR_ID = "8a1d6c52-77e0-4b9d-8e7a-2c3f4d5e6f70"
        const val MEMBER_ID = 7L
        val FIRST: LocalDateTime = LocalDateTime.of(2026, 11, 24, 10, 0)
    }
}
