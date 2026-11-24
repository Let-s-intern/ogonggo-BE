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
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(ContentEventAppender::class, ContentEventRemover::class)
internal class ContentEventPersistenceTest @Autowired constructor(
    private val contentEventAppender: ContentEventAppender,
    private val contentEventRemover: ContentEventRemover,
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

    @Test
    fun `취준고민과 모집글의 조회도 대상 종류 이름으로 저장한다`() {
        // given
        val concernView = ContentEventAppendDto(
            eventType = ContentEventType.VIEW,
            targetType = ContentEventTargetType.CONCERN,
            targetId = 40L,
            exposureId = null,
            userId = null,
            visitorId = VISITOR_ID,
            source = ContentEventSource.LIST,
        )
        val postView = concernView.copy(targetType = ContentEventTargetType.RECRUITMENT_POST, targetId = 50L)

        // when
        contentEventAppender.appendAll(listOf(concernView, postView), OCCURRED_AT)

        // then
        val saved = contentEventRepository.findAll().sortedBy { it.id }
        assertEquals(listOf(ContentEventTargetType.CONCERN, ContentEventTargetType.RECRUITMENT_POST), saved.map { it.targetType })
        assertEquals(listOf(40L, 50L), saved.map { it.targetId })
    }

    @Test
    fun `보관 기간 365일이 지난 이벤트만 묶음마다 지운다`() {
        // given
        val today = LocalDate.of(2026, 11, 19)
        val cutoff = today.minusDays(365).atStartOfDay()
        val event = ContentEventAppendDto(
            eventType = ContentEventType.VIEW,
            targetType = ContentEventTargetType.JOB,
            targetId = 15818L,
            exposureId = null,
            userId = null,
            visitorId = VISITOR_ID,
            source = ContentEventSource.LIST,
        )
        contentEventAppender.appendAll(listOf(event, event), cutoff.minusNanos(1_000))
        contentEventAppender.appendAll(listOf(event), cutoff)

        // when
        val firstBatch = contentEventRemover.removeExpired(today, batchSize = 1)
        val secondBatch = contentEventRemover.removeExpired(today, batchSize = 1)
        val thirdBatch = contentEventRemover.removeExpired(today, batchSize = 1)

        // then
        assertEquals(listOf(1, 1, 0), listOf(firstBatch, secondBatch, thirdBatch))
        assertEquals(listOf(cutoff), contentEventRepository.findAll().map { it.occurredAt })
    }

    private companion object {
        const val VISITOR_ID = "5f0c2e9e-1b7a-4c1e-9a43-0d2f3b6c7a11"
        val OCCURRED_AT: LocalDateTime = LocalDateTime.of(2026, 11, 19, 10, 0, 0)
    }
}
