package com.ogonggo.userapi.contentevent.business

import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentevent.implement.ContentEventAppender
import com.ogonggo.core.contentevent.implement.ContentEventReader
import com.ogonggo.core.contentevent.implement.ContentEventValidator
import com.ogonggo.core.contentevent.implement.dto.ContentEventAppendDto
import com.ogonggo.userapi.bootcamp.business.BootcampViewedEvent
import com.ogonggo.userapi.concern.business.ConcernViewedEvent
import com.ogonggo.userapi.contentevent.implement.ContentEventRequestGuard
import com.ogonggo.userapi.job.business.JobViewedEvent
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostViewedEvent
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class UserContentEventServiceTest {

    private val validator = Mockito.mock(ContentEventValidator::class.java)
    private val appender = Mockito.mock(ContentEventAppender::class.java)
    private val reader = Mockito.mock(ContentEventReader::class.java)
    private val publisher = Mockito.mock(ApplicationEventPublisher::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-11-19T01:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = UserContentEventService(validator, appender, reader, ContentEventRequestGuard(clock), publisher, clock)

    @Test
    fun `봇 User-Agent 요청은 아무것도 저장하지 않는다`() {
        // when
        service.collect(null, command(userAgent = "Mozilla/5.0 (compatible; Googlebot/2.1)"))

        // then
        Mockito.verifyNoInteractions(validator, appender, reader, publisher)
    }

    @Test
    fun `방문자의 분당 요청 상한을 넘은 요청은 저장하지 않는다`() {
        // given
        repeat(ContentEventRequestGuard.MAX_REQUESTS_PER_VISITOR) { service.collect(null, command()) }
        Mockito.clearInvocations(validator, appender, reader, publisher)

        // when
        service.collect(null, command())

        // then
        Mockito.verifyNoInteractions(validator, appender, reader, publisher)
    }

    @Test
    fun `조회수에 넣을 조회만 대상 종류에 맞는 조회 사실로 발행한다`() {
        // given
        val storable = listOf(
            dto(ContentEventTargetType.JOB, 1L),
            dto(ContentEventTargetType.BOOTCAMP, 2L),
            dto(ContentEventTargetType.CONCERN, 3L),
            dto(ContentEventTargetType.RECRUITMENT_POST, 4L),
            dto(ContentEventTargetType.JOB, 5L),
        )
        val countable = storable.take(4)
        Mockito.`when`(validator.filterStorable(listOf(requested), NOW)).thenReturn(storable)
        Mockito.`when`(reader.filterCountableViews(storable, NOW)).thenReturn(countable)

        // when
        service.collect(null, command())

        // then
        val inOrder = Mockito.inOrder(reader, appender, publisher)
        inOrder.verify(reader).filterCountableViews(storable, NOW)
        inOrder.verify(appender).appendAll(storable, NOW)
        inOrder.verify(publisher).publishEvent(JobViewedEvent(1L))
        inOrder.verify(publisher).publishEvent(BootcampViewedEvent(2L))
        inOrder.verify(publisher).publishEvent(ConcernViewedEvent(3L))
        inOrder.verify(publisher).publishEvent(RecruitmentPostViewedEvent(4L))
        Mockito.verifyNoMoreInteractions(publisher)
    }

    @Test
    fun `조회수에 넣을 조회가 없으면 이벤트는 저장하되 아무것도 발행하지 않는다`() {
        // given
        val storable = listOf(dto(ContentEventTargetType.JOB, 1L))
        Mockito.`when`(validator.filterStorable(listOf(requested), NOW)).thenReturn(storable)
        Mockito.`when`(reader.filterCountableViews(storable, NOW)).thenReturn(emptyList())

        // when
        service.collect(null, command())

        // then
        Mockito.verify(appender).appendAll(storable, NOW)
        Mockito.verifyNoInteractions(publisher)
    }

    private val requested = dto(ContentEventTargetType.JOB, 15818L)

    private fun dto(targetType: ContentEventTargetType, targetId: Long) = ContentEventAppendDto(
        eventType = ContentEventType.VIEW,
        targetType = targetType,
        targetId = targetId,
        exposureId = null,
        userId = null,
        visitorId = "5f0c2e9e-1b7a-4c1e-9a43-0d2f3b6c7a11",
        source = ContentEventSource.LIST,
    )

    private fun command(userAgent: String = BROWSER) = CollectContentEventsCommand(
        visitorId = "5f0c2e9e-1b7a-4c1e-9a43-0d2f3b6c7a11",
        events = listOf(
            CollectContentEventCommand(ContentEventType.VIEW, ContentEventTargetType.JOB, 15818L, null, ContentEventSource.LIST, null, null),
        ),
        clientIp = "203.0.113.7",
        userAgent = userAgent,
    )

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 19, 10, 0)
        const val BROWSER = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/131.0 Safari/537.36"
    }
}
