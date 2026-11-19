package com.ogonggo.core.contentevent.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ContentEventDomainTest {

    @Test
    fun `스크롤 위치는 0에서 100 사이다`() {
        assertEquals(0, scroll(scrollPercent = 0, durationMs = 0).scrollPercent)
        assertEquals(100, scroll(scrollPercent = 100, durationMs = 1).scrollPercent)
        assertThrows(IllegalArgumentException::class.java) { scroll(scrollPercent = -1, durationMs = 0) }
        assertThrows(IllegalArgumentException::class.java) { scroll(scrollPercent = 101, durationMs = 0) }
    }

    @Test
    fun `보인 시간은 0 이상이다`() {
        assertThrows(IllegalArgumentException::class.java) { scroll(scrollPercent = 50, durationMs = -1) }
    }

    @Test
    fun `스크롤 위치와 보인 시간은 스크롤 이벤트에만 싣는다`() {
        assertThrows(IllegalArgumentException::class.java) {
            event(ContentEventType.VIEW, scrollPercent = 50)
        }
    }

    @Test
    fun `상세 화면 이벤트는 대상 공고가 있어야 한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            event(ContentEventType.VIEW, targetType = null, targetId = null)
        }
    }

    @Test
    fun `광고 소재 이벤트는 노출이 있어야 하고 배너는 대상 공고가 없어도 된다`() {
        assertThrows(IllegalArgumentException::class.java) {
            event(ContentEventType.IMPRESSION, exposureId = null)
        }

        val bannerClick = event(ContentEventType.AD_CLICK, targetType = null, targetId = null, exposureId = 12L)

        assertEquals(12L, bannerClick.exposureId)
    }

    @Test
    fun `대상 종류와 식별자는 함께 있어야 한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            event(ContentEventType.IMPRESSION, targetType = ContentEventTargetType.JOB, targetId = null, exposureId = 12L)
        }
    }

    @Test
    fun `방문자 식별자는 36자 이하다`() {
        assertThrows(IllegalArgumentException::class.java) { event(ContentEventType.VIEW, visitorId = "a".repeat(37)) }
        assertThrows(IllegalArgumentException::class.java) { event(ContentEventType.VIEW, visitorId = " ") }
    }

    private fun scroll(scrollPercent: Int, durationMs: Long) =
        event(ContentEventType.SCROLL, scrollPercent = scrollPercent, durationMs = durationMs)

    private fun event(
        eventType: ContentEventType,
        targetType: ContentEventTargetType? = ContentEventTargetType.JOB,
        targetId: Long? = 15818L,
        exposureId: Long? = null,
        visitorId: String = VISITOR_ID,
        scrollPercent: Int? = null,
        durationMs: Long? = null,
    ) = ContentEvent(
        eventType = eventType,
        targetType = targetType,
        targetId = targetId,
        exposureId = exposureId,
        userId = null,
        visitorId = visitorId,
        source = ContentEventSource.LIST,
        scrollPercent = scrollPercent,
        durationMs = durationMs,
        occurredAt = LocalDateTime.of(2026, 11, 19, 10, 0),
    )

    private companion object {
        const val VISITOR_ID = "5f0c2e9e-1b7a-4c1e-9a43-0d2f3b6c7a11"
    }
}
