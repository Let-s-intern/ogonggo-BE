package com.ogonggo.userapi.contentevent.implement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ContentEventRequestGuardTest {

    @Test
    fun `알려진 봇과 User-Agent가 없는 요청은 받지 않는다`() {
        val guard = ContentEventRequestGuard(clockAt(START))

        listOf(
            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
            "Mozilla/5.0 (compatible; Yeti/1.1; +http://naver.me/spd)",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 HeadlessChrome/120.0 Safari/537.36",
            "curl/8.4.0",
            "",
            null,
        ).forEach { assertFalse(guard.isAllowed(VISITOR_ID, IP, it), "$it") }
        assertTrue(guard.isAllowed(VISITOR_ID, IP, BROWSER))
    }

    @Test
    fun `같은 방문자가 1분에 상한을 넘게 보내면 넘친 요청은 받지 않고 다음 1분에는 다시 받는다`() {
        // given
        val clock = MovableClock(START)
        val guard = ContentEventRequestGuard(clock)

        // when
        val results = (1..ContentEventRequestGuard.MAX_REQUESTS_PER_VISITOR + 1).map { guard.isAllowed(VISITOR_ID, IP, BROWSER) }

        // then
        assertEquals(ContentEventRequestGuard.MAX_REQUESTS_PER_VISITOR, results.count { it })
        assertFalse(results.last())
        assertTrue(guard.isAllowed(OTHER_VISITOR_ID, IP, BROWSER))
        clock.instant = START.plusSeconds(60)
        assertTrue(guard.isAllowed(VISITOR_ID, IP, BROWSER))
    }

    @Test
    fun `같은 IP가 1분에 상한을 넘게 보내면 방문자가 달라도 받지 않는다`() {
        val guard = ContentEventRequestGuard(clockAt(START))

        val results = (1..ContentEventRequestGuard.MAX_REQUESTS_PER_IP + 1).map { guard.isAllowed("visitor-$it", IP, BROWSER) }

        assertEquals(ContentEventRequestGuard.MAX_REQUESTS_PER_IP, results.count { it })
        assertFalse(results.last())
        assertTrue(guard.isAllowed(OTHER_VISITOR_ID, "198.51.100.2", BROWSER))
    }

    private fun clockAt(instant: Instant): Clock = Clock.fixed(instant, SEOUL)

    private class MovableClock(var instant: Instant) : Clock() {
        override fun getZone(): ZoneId = SEOUL
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = instant
    }

    private companion object {
        val START: Instant = Instant.parse("2026-11-19T01:00:00Z")
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        const val VISITOR_ID = "5f0c2e9e-1b7a-4c1e-9a43-0d2f3b6c7a11"
        const val OTHER_VISITOR_ID = "6a1d3f0f-2c8b-4d2f-8b54-1e3a4c7d8b22"
        const val IP = "203.0.113.7"
        const val BROWSER = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/131.0 Safari/537.36"
    }
}
