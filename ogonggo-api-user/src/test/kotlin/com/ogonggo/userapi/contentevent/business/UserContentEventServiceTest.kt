package com.ogonggo.userapi.contentevent.business

import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentevent.implement.ContentEventAppender
import com.ogonggo.core.contentevent.implement.ContentEventValidator
import com.ogonggo.userapi.contentevent.implement.ContentEventRequestGuard
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class UserContentEventServiceTest {

    private val validator = Mockito.mock(ContentEventValidator::class.java)
    private val appender = Mockito.mock(ContentEventAppender::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-11-19T01:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = UserContentEventService(validator, appender, ContentEventRequestGuard(clock), clock)

    @Test
    fun `봇 User-Agent 요청은 아무것도 저장하지 않는다`() {
        // when
        service.collect(null, command(userAgent = "Mozilla/5.0 (compatible; Googlebot/2.1)"))

        // then
        Mockito.verifyNoInteractions(validator, appender)
    }

    @Test
    fun `방문자의 분당 요청 상한을 넘은 요청은 저장하지 않는다`() {
        // given
        repeat(ContentEventRequestGuard.MAX_REQUESTS_PER_VISITOR) { service.collect(null, command()) }
        Mockito.clearInvocations(validator, appender)

        // when
        service.collect(null, command())

        // then
        Mockito.verifyNoInteractions(validator, appender)
    }

    private fun command(userAgent: String = BROWSER) = CollectContentEventsCommand(
        visitorId = "5f0c2e9e-1b7a-4c1e-9a43-0d2f3b6c7a11",
        events = listOf(
            CollectContentEventCommand(ContentEventType.VIEW, ContentEventTargetType.JOB, 15818L, null, ContentEventSource.LIST, null, null),
        ),
        clientIp = "203.0.113.7",
        userAgent = userAgent,
    )

    private companion object {
        const val BROWSER = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/131.0 Safari/537.36"
    }
}
