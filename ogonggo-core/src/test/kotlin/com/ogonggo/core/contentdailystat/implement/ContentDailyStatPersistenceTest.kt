package com.ogonggo.core.contentdailystat.implement

import com.ogonggo.core.contentdailystat.domain.ContentDailyStat
import com.ogonggo.core.contentdailystat.domain.ContentStatKey
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricDto
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricScope
import com.ogonggo.core.contentdailystat.persistence.ContentDailyStatJpaRepository
import com.ogonggo.core.contentdailystat.persistence.ContentDailyStatQueryRepository
import com.ogonggo.core.contentevent.domain.ContentEvent
import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentevent.persistence.ContentEventJpaRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
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
    private val contentDailyStatQueryRepository: ContentDailyStatQueryRepository,
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
        val written = contentDailyStatManager.rebuild(DAY, TODAY)

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
        contentDailyStatManager.rebuild(DAY, TODAY)
        val first = snapshot()

        // when
        contentDailyStatManager.rebuild(DAY, TODAY)

        // then
        assertEquals(first, snapshot())
    }

    @Test
    fun `직전 조회에서 1분 안의 재조회는 세지 않고 정확히 1분이 지나면 센다(세지 않은 조회도 기준이 된다)`() {
        // given
        save(view(at(10, 0)))
        save(view(at(10, 0).plusSeconds(59)))
        save(view(at(10, 1).plusSeconds(58)))
        save(view(at(10, 2).plusSeconds(58)))

        // when
        contentDailyStatManager.rebuild(DAY, TODAY)

        // then
        assertEquals(2L, contentDailyStatRepository.findAll().single().views)
    }

    @Test
    fun `자정을 넘긴 재조회는 전날 끝 1분의 조회를 보고 세지 않는다`() {
        // given
        save(view(DAY.minusDays(1).atTime(23, 59, 30)))
        save(view(at(0, 0).plusSeconds(20)))
        save(view(at(0, 0).plusSeconds(20), visitor = "other"))

        // when
        contentDailyStatManager.rebuild(DAY, TODAY)

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
        contentDailyStatManager.rebuild(DAY, TODAY)
        contentDailyStatManager.rebuild(DAY.plusDays(1), TODAY)

        // then
        val byDate = contentDailyStatRepository.findAll().associate { it.statDate to (it.impressions to it.uniqueImpressions) }
        assertEquals(3L to 2L, byDate[DAY])
        assertEquals(1L to 1L, byDate[DAY.plusDays(1)])
    }

    @Test
    fun `원본 보관 기간이 지난 날은 거부하고 기존 집계 행을 지우지 않는다`() {
        // given
        val expired = ContentEvent.retainedSince(TODAY).minusDays(1)
        contentDailyStatRepository.save(ContentDailyStat.of(expired, ContentStatKey(ContentEventTargetType.JOB, JOB_ID, EXPOSURE_ID, ContentEventSource.AD_CARD), ContentMetricDto.ZERO.copy(views = 7L)))

        // when
        val error = assertThrows(IllegalArgumentException::class.java) { contentDailyStatManager.rebuild(expired, TODAY) }

        // then
        assertTrue(error.message!!.contains(expired.toString()))
        assertEquals(listOf(7L), contentDailyStatRepository.findAll().map { it.views })
    }

    @Test
    fun `원본 보관 시작일 당일은 다시 집계한다`() {
        // given
        val firstRetained = ContentEvent.retainedSince(TODAY)
        save(view(firstRetained.atTime(10, 0)))

        // when
        val written = contentDailyStatManager.rebuild(firstRetained, TODAY)

        // then
        assertEquals(1, written)
        assertEquals(firstRetained, contentDailyStatRepository.findAll().single().statDate)
    }

    @Test
    fun `같은 브라우저의 회원 행과 비회원 행은 한 방문자라 순 방문자와 CTA 클릭 방문자가 1이다`() {
        // given
        save(view(at(10, 0), visitor = "a"))
        save(view(at(11, 0), userId = 7L, browser = "a"))
        save(event(ContentEventType.CTA_CLICK, at(11, 1), userId = 7L, browser = "a"))
        save(event(ContentEventType.CTA_CLICK, at(11, 2), visitor = "a"))

        // when
        contentDailyStatManager.rebuild(DAY, TODAY)

        // then
        val stat = contentDailyStatRepository.findAll().single()
        assertEquals(2L, stat.views)
        assertEquals(1L, stat.uniqueVisitors)
        assertEquals(1L, stat.memberUniqueVisitors)
        assertEquals(2L, stat.ctaClicks)
        assertEquals(1L, stat.ctaUniqueVisitors)
    }

    @Test
    fun `회원 키로 본 조회와 비회원 키로 본 재조회도 1분 안이면 한 번이다`() {
        // given
        save(view(at(10, 0), userId = 7L, browser = "a"))
        save(view(at(10, 0).plusSeconds(20), visitor = "a"))
        save(view(at(10, 0).plusSeconds(40), visitor = "a"))

        // when
        contentDailyStatManager.rebuild(DAY, TODAY)

        // then
        val stat = contentDailyStatRepository.findAll().single()
        assertEquals(1L, stat.views)
        assertEquals(1L, stat.memberViews)
        assertEquals(1L, stat.uniqueVisitors)
    }

    @Test
    fun `조회는 비회원 키로 하고 스크롤은 회원 키로 보낸 방문자의 스크롤이 도달에 들어간다`() {
        // given
        save(view(at(10, 0), visitor = "a"))
        save(event(ContentEventType.SCROLL, at(10, 5), userId = 7L, browser = "a", scrollPercent = 80, durationMs = 20_000L))
        save(view(at(10, 0), visitor = "b"))

        // when
        contentDailyStatManager.rebuild(DAY, TODAY)

        // then
        val stat = contentDailyStatRepository.findAll().single()
        assertEquals(2L, stat.uniqueVisitors)
        assertEquals(listOf(1L, 1L, 1L, 0L), listOf(stat.scrollReach25, stat.scrollReach50, stat.scrollReach75, stat.scrollReach100))
    }

    @Test
    fun `같은 브라우저의 회원 노출과 비회원 노출은 순 노출수 1이고 회원 없는 브라우저는 따로 센다`() {
        // given
        save(ad(ContentEventType.IMPRESSION, at(9, 0)))
        save(ad(ContentEventType.IMPRESSION, at(9, 1)).copyAsMember(7L, "a"))
        save(ad(ContentEventType.IMPRESSION, at(9, 2)).copyAsVisitor("b"))
        save(ad(ContentEventType.IMPRESSION, at(9, 3)).copyAsVisitor("c"))

        // when
        contentDailyStatManager.rebuild(DAY, TODAY)

        // then
        val banner = contentDailyStatRepository.findAll().single()
        assertEquals(4L, banner.impressions)
        assertEquals(3L, banner.uniqueImpressions)
    }

    @Test
    fun `도달과 광고 클릭 방문자도 같은 브라우저의 회원과 비회원을 한 사람으로 센다`() {
        // given
        save(ad(ContentEventType.IMPRESSION, at(9, 0)))
        save(ad(ContentEventType.IMPRESSION, at(9, 1)).copyAsMember(7L, "a"))
        save(ad(ContentEventType.IMPRESSION, at(9, 2)).copyAsVisitor("b"))
        save(ad(ContentEventType.AD_CLICK, at(9, 3)))
        save(ad(ContentEventType.AD_CLICK, at(9, 4)).copyAsMember(7L, "a"))

        // when
        val scope = ContentMetricScope.Exposures(setOf(BANNER_EXPOSURE_ID))
        val impressionVisitors = contentDailyStatQueryRepository.countDistinctVisitors(scope, ContentEventType.IMPRESSION, DAY.atStartOfDay(), DAY.plusDays(1).atStartOfDay())
        val clickVisitors = contentDailyStatQueryRepository.countDistinctVisitors(scope, ContentEventType.AD_CLICK, DAY.atStartOfDay(), DAY.plusDays(1).atStartOfDay())

        // then
        assertEquals(2L, impressionVisitors)
        assertEquals(1L, clickVisitors)
    }

    @Test
    fun `방문자와 회원을 잇는 표는 계산 범위 안의 이벤트로만 만든다`() {
        // given
        save(view(at(10, 0), visitor = "a"))
        save(view(at(10, 0), userId = 7L))
        save(view(DAY.plusDays(1).atTime(10, 0), userId = 7L, browser = "a"))

        // when
        contentDailyStatManager.rebuild(DAY, TODAY)

        // then
        assertEquals(2L, contentDailyStatRepository.findAll().single().uniqueVisitors)
    }

    private fun ContentEvent.copyAsMember(userId: Long, browser: String) = ContentEvent(
        eventType = eventType,
        targetType = targetType,
        targetId = targetId,
        exposureId = exposureId,
        userId = userId,
        visitorId = browser,
        source = source,
        scrollPercent = scrollPercent,
        durationMs = durationMs,
        occurredAt = occurredAt,
    )

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
        browser: String? = null,
    ) = event(ContentEventType.VIEW, occurredAt, visitor, userId, exposureId, source, browser = browser)

    private fun event(
        type: ContentEventType,
        occurredAt: LocalDateTime,
        visitor: String = "a",
        userId: Long? = null,
        exposureId: Long? = EXPOSURE_ID,
        source: ContentEventSource = ContentEventSource.AD_CARD,
        scrollPercent: Int? = null,
        durationMs: Long? = null,
        browser: String? = null,
    ) = ContentEvent(
        eventType = type,
        targetType = ContentEventTargetType.JOB,
        targetId = JOB_ID,
        exposureId = exposureId,
        userId = userId,
        visitorId = browser ?: if (userId != null) "member-browser" else visitor,
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
        val TODAY: LocalDate = LocalDate.of(2026, 11, 25)
        const val JOB_ID = 15818L
        const val EXPOSURE_ID = 31L
        const val BANNER_EXPOSURE_ID = 12L
    }
}
