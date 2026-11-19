package com.ogonggo.core.contentevent.implement

import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentevent.implement.dto.ContentEventAppendDto
import com.ogonggo.core.contentevent.persistence.ContentEventJpaRepository
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
@Import(ContentEventAppender::class)
internal class ContentEventPersistenceTest @Autowired constructor(
    private val contentEventAppender: ContentEventAppender,
    private val contentEventRepository: ContentEventJpaRepository,
) {

    @Test
    fun `한 요청의 이벤트를 받은 시각으로 모두 저장한다`() {
        // given
        val view = ContentEventAppendDto(
            eventType = ContentEventType.VIEW,
            targetType = ContentEventTargetType.JOB,
            targetId = 15818L,
            exposureId = 31L,
            userId = 7L,
            visitorId = VISITOR_ID,
            source = ContentEventSource.AD_CARD,
        )
        val scroll = view.copy(eventType = ContentEventType.SCROLL, exposureId = null, userId = null, scrollPercent = 72, durationMs = 48_000L)
        val bannerClick = view.copy(
            eventType = ContentEventType.AD_CLICK,
            targetType = null,
            targetId = null,
            exposureId = 12L,
            source = ContentEventSource.BANNER,
        )

        // when
        contentEventAppender.appendAll(listOf(view, view, scroll, bannerClick), OCCURRED_AT)

        // then
        val saved = contentEventRepository.findAll().sortedBy { it.id }
        assertEquals(
            listOf(ContentEventType.VIEW, ContentEventType.VIEW, ContentEventType.SCROLL, ContentEventType.AD_CLICK),
            saved.map { it.eventType },
        )
        assertEquals(setOf(OCCURRED_AT), saved.map { it.occurredAt }.toSet())
        assertEquals(72, saved[2].scrollPercent)
        assertEquals(48_000L, saved[2].durationMs)
        assertEquals(null, saved[3].targetId)
        assertEquals(7L, saved[0].userId)
    }

    private companion object {
        const val VISITOR_ID = "5f0c2e9e-1b7a-4c1e-9a43-0d2f3b6c7a11"
        val OCCURRED_AT: LocalDateTime = LocalDateTime.of(2026, 11, 19, 10, 0, 0)
    }
}
