package com.ogonggo.core.contentdailystat.implement

import com.ogonggo.core.contentdailystat.domain.ContentDailyStat
import com.ogonggo.core.contentdailystat.persistence.ContentDailyStatJpaRepository
import com.ogonggo.core.contentdailystat.persistence.ContentDailyStatQueryRepository
import com.ogonggo.core.contentevent.domain.ContentEvent
import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
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
@Import(ContentDailyStatManager::class, ContentDailyStatQueryRepository::class)
internal class ContentDailyStatPersistenceTest @Autowired constructor(
    private val contentDailyStatManager: ContentDailyStatManager,
    private val contentDailyStatRepository: ContentDailyStatJpaRepository,
    private val contentEventRepository: ContentEventJpaRepository,
) {

    @Test
    fun `하루 이벤트를 대상·노출·유입 경로마다 한 행으로 모은다`() {
        // given
        save(view(at(10, 0), userId = 7L))
        save(view(at(11, 0), visitor = "b"))
        save(event(ContentEventType.CTA_CLICK, at(11, 1), visitor = "b"))
        save(event(ContentEventType.SCROLL, at(11, 2), visitor = "b", scrollPercent = 80, durationMs = 30_000L))
        save(event(ContentEventType.SCROLL, at(10, 5), userId = 7L, scrollPercent = 40, durationMs = 10_000L))
        save(view(at(12, 0), visitor = "c", exposureId = null, source = ContentEventSource.LIST))
        save(ad(ContentEventType.IMPRESSION, at(9, 0)))
        save(ad(ContentEventType.IMPRESSION, at(9, 1)))
        save(ad(ContentEventType.AD_CLICK, at(9, 2)))
        save(view(DAY.plusDays(1).atTime(0, 30), visitor = "d"))

        // when
        val written = contentDailyStatManager.rebuild(DAY)

        // then
        val stats = contentDailyStatRepository.findAll()
        assertEquals(3, written)
        val adCard = stats.single { it.exposureId == EXPOSURE_ID && it.targetId == JOB_ID }
        assertEquals(2L, adCard.views)
        assertEquals(2L, adCard.uniqueVisitors)
        assertEquals(1L, adCard.memberViews)
        assertEquals(1L, adCard.memberUniqueVisitors)
        assertEquals(1L, adCard.ctaClicks)
        assertEquals(1L, adCard.ctaUniqueVisitors)
        assertEquals(0L, adCard.ctaUniqueMembers)
        assertEquals(2L, adCard.scrollCount)
        assertEquals(120L, adCard.scrollPercentSum)
        assertEquals(40_000L, adCard.durationMsSum)
        assertEquals(listOf(2L, 1L, 1L, 0L), listOf(adCard.scrollReach25, adCard.scrollReach50, adCard.scrollReach75, adCard.scrollReach100))
        val list = stats.single { it.source == ContentEventSource.LIST }
        assertEquals(1L, list.views)
        val banner = stats.single { it.targetId == null }
        assertEquals(2L, banner.impressions)
        assertEquals(1L, banner.adClicks)
    }

    @Test
    fun `같은 날을 다시 모아도 행이 늘지 않고 값이 같다`() {
        // given
        save(view(at(10, 0)))
        save(ad(ContentEventType.IMPRESSION, at(9, 0)))
        contentDailyStatManager.rebuild(DAY)
        val first = snapshot()

        // when
        contentDailyStatManager.rebuild(DAY)

        // then
        assertEquals(first, snapshot())
    }

    @Test
    fun `직전 조회에서 30분 안의 재조회는 세지 않고 정확히 30분이 지나면 센다(세지 않은 조회도 기준이 된다)`() {
        // given
        save(view(at(10, 0)))
        save(view(at(10, 29).plusSeconds(59)))
        save(view(at(10, 59).plusSeconds(58)))
        save(view(at(11, 29).plusSeconds(58)))

        // when
        contentDailyStatManager.rebuild(DAY)

        // then
        assertEquals(2L, contentDailyStatRepository.findAll().single().views)
    }

    @Test
    fun `자정을 넘긴 재조회는 전날 끝 30분의 조회를 보고 세지 않는다`() {
        // given
        save(view(DAY.minusDays(1).atTime(23, 50)))
        save(view(at(0, 10)))
        save(view(at(0, 10), visitor = "other"))

        // when
        contentDailyStatManager.rebuild(DAY)

        // then
        val stat = contentDailyStatRepository.findAll().single()
        assertEquals(1L, stat.views)
        assertEquals(2L, stat.uniqueVisitors)
    }

    @Test
    fun `같은 방문자가 하루에 두 번 본 노출은 순 노출 1이고 다음 날 다시 보면 그날 1이다`() {
        // given
        save(ad(ContentEventType.IMPRESSION, at(9, 0)))
        save(ad(ContentEventType.IMPRESSION, at(18, 0)))
        save(ad(ContentEventType.IMPRESSION, at(18, 1)).copyAsVisitor("other"))
        save(ad(ContentEventType.IMPRESSION, DAY.plusDays(1).atTime(9, 0)))

        // when
        contentDailyStatManager.rebuild(DAY)
        contentDailyStatManager.rebuild(DAY.plusDays(1))

        // then
        val byDate = contentDailyStatRepository.findAll().associate { it.statDate to (it.impressions to it.uniqueImpressions) }
        assertEquals(3L to 2L, byDate[DAY])
        assertEquals(1L to 1L, byDate[DAY.plusDays(1)])
    }

    private fun ContentEvent.copyAsVisitor(visitor: String) = ContentEvent(
        eventType = eventType,
        targetType = targetType,
        targetId = targetId,
        exposureId = exposureId,
        userId = userId,
        visitorId = visitor,
        source = source,
        scrollPercent = scrollPercent,
        durationMs = durationMs,
        occurredAt = occurredAt,
    )

    private fun snapshot() = contentDailyStatRepository.findAll().map { it.copyValues() }.sortedBy { it.toString() }

    private fun ContentDailyStat.copyValues() = listOf(statDate, targetType, targetId, exposureId, source) + toMetric().toString()

    private fun save(event: ContentEvent) = contentEventRepository.save(event)

    private fun at(hour: Int, minute: Int): LocalDateTime = DAY.atTime(hour, minute)

    private fun view(
        occurredAt: LocalDateTime,
        visitor: String = "a",
        userId: Long? = null,
        exposureId: Long? = EXPOSURE_ID,
        source: ContentEventSource = ContentEventSource.AD_CARD,
    ) = event(ContentEventType.VIEW, occurredAt, visitor, userId, exposureId, source)

    private fun event(
        type: ContentEventType,
        occurredAt: LocalDateTime,
        visitor: String = "a",
        userId: Long? = null,
        exposureId: Long? = EXPOSURE_ID,
        source: ContentEventSource = ContentEventSource.AD_CARD,
        scrollPercent: Int? = null,
        durationMs: Long? = null,
    ) = ContentEvent(
        eventType = type,
        targetType = ContentEventTargetType.JOB,
        targetId = JOB_ID,
        exposureId = exposureId,
        userId = userId,
        visitorId = if (userId != null) "member-browser" else visitor,
        source = source,
        scrollPercent = scrollPercent,
        durationMs = durationMs,
        occurredAt = occurredAt,
    )

    private fun ad(type: ContentEventType, occurredAt: LocalDateTime) = ContentEvent(
        eventType = type,
        targetType = null,
        targetId = null,
        exposureId = BANNER_EXPOSURE_ID,
        userId = null,
        visitorId = "a",
        source = ContentEventSource.BANNER,
        scrollPercent = null,
        durationMs = null,
        occurredAt = occurredAt,
    )

    private companion object {
        val DAY: LocalDate = LocalDate.of(2026, 11, 18)
        const val JOB_ID = 15818L
        const val EXPOSURE_ID = 31L
        const val BANNER_EXPOSURE_ID = 12L
    }
}
