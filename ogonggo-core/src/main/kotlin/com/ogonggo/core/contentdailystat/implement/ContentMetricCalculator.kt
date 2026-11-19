package com.ogonggo.core.contentdailystat.implement

import com.ogonggo.core.contentdailystat.domain.ContentStatKey
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricDto
import com.ogonggo.core.contentdailystat.persistence.ContentAdEventCount
import com.ogonggo.core.contentdailystat.persistence.ContentEventRow
import com.ogonggo.core.contentevent.domain.ContentEventType
import java.time.Duration
import java.time.LocalDateTime
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 원본 이벤트로 지표를 센다. 일별 집계와 리포트의 오늘 숫자가 같은 규칙으로 세도록 한 곳에 둔다.
 *
 * - 방문자는 회원이면 회원 식별자, 아니면 익명 방문자 식별자다.
 * - 같은 방문자가 같은 대상을 직전 조회로부터 30분 안에 다시 열면 세지 않는다. 직전 조회가 세지 않은 조회여도 기준이 된다.
 *   30분이 정확히 지난 조회는 센다. 하루 첫 30분은 전날 끝 30분의 조회를 보고 가른다([from] 앞의 행은 이 판단에만 쓴다).
 * - 30분 안 재조회가 다른 유입 경로로 들어와도 세지 않는다. 센 조회는 그 조회의 유입 경로·노출로 들어간다.
 */
internal object ContentMetricCalculator {

    val DUPLICATE_VIEW_WINDOW: Duration = Duration.ofMinutes(30)

    fun <K> measure(rows: List<ContentEventRow>, from: LocalDateTime, keyOf: (ContentEventRow) -> K): Map<K, ContentMetricDto> {
        val countedViews = countedViews(rows)
        val inRange = rows.filter { !it.occurredAt.isBefore(from) }
        return inRange.groupBy(keyOf).mapValues { (_, keyRows) -> measureRows(keyRows, countedViews) }
    }

    fun measureAll(rows: List<ContentEventRow>, from: LocalDateTime): ContentMetricDto =
        measure(rows, from) { Unit }[Unit] ?: ContentMetricDto.ZERO

    fun statKeyOf(row: ContentEventRow) = ContentStatKey(row.targetType, row.targetId, row.exposureId, row.source)

    fun adMetricsByKey(counts: List<ContentAdEventCount>): Map<ContentStatKey, ContentMetricDto> =
        counts.groupBy { ContentStatKey(it.targetType, it.targetId, it.exposureId, it.source) }
            .mapValues { (_, keyCounts) ->
                ContentMetricDto(
                    impressions = keyCounts.filter { it.eventType == ContentEventType.IMPRESSION }.sumOf { it.count },
                    adClicks = keyCounts.filter { it.eventType == ContentEventType.AD_CLICK }.sumOf { it.count },
                )
            }

    private fun countedViews(rows: List<ContentEventRow>): Set<ContentEventRow> {
        val lastViewAt = mutableMapOf<Triple<String, Any?, Long?>, LocalDateTime>()
        // 같은 순간에 같은 값으로 두 번 온 조회를 가르려고 값이 아니라 객체로 기억한다.
        val counted: MutableSet<ContentEventRow> = Collections.newSetFromMap(IdentityHashMap())
        rows.filter { it.eventType == ContentEventType.VIEW }
            .sortedBy { it.occurredAt }
            .forEach { row ->
                val key = Triple(visitorOf(row), row.targetType, row.targetId)
                val previous = lastViewAt[key]
                if (previous == null || !row.occurredAt.isBefore(previous.plus(DUPLICATE_VIEW_WINDOW))) {
                    counted += row
                }
                lastViewAt[key] = row.occurredAt
            }
        return counted
    }

    private fun measureRows(rows: List<ContentEventRow>, countedViews: Set<ContentEventRow>): ContentMetricDto {
        val views = rows.filter { it.eventType == ContentEventType.VIEW }
        val counted = views.filter { it in countedViews }
        val ctaClicks = rows.filter { it.eventType == ContentEventType.CTA_CLICK }
        val scrolls = rows.filter { it.eventType == ContentEventType.SCROLL }
        val viewVisitors = views.map(::visitorOf).toSet()
        val maxScrollByVisitor = scrolls.groupBy(::visitorOf)
            .filterKeys { it in viewVisitors }
            .mapValues { (_, visitorScrolls) -> visitorScrolls.maxOf { it.scrollPercent ?: 0 } }
        fun reach(threshold: Int) = maxScrollByVisitor.values.count { it >= threshold }.toLong()
        return ContentMetricDto(
            views = counted.size.toLong(),
            uniqueVisitors = viewVisitors.size.toLong(),
            memberViews = counted.count { it.userId != null }.toLong(),
            memberUniqueVisitors = views.mapNotNull { it.userId }.toSet().size.toLong(),
            ctaClicks = ctaClicks.size.toLong(),
            ctaUniqueVisitors = ctaClicks.map(::visitorOf).toSet().size.toLong(),
            ctaUniqueMembers = ctaClicks.mapNotNull { it.userId }.toSet().size.toLong(),
            scrollCount = scrolls.size.toLong(),
            scrollPercentSum = scrolls.sumOf { (it.scrollPercent ?: 0).toLong() },
            durationMsSum = scrolls.sumOf { it.durationMs ?: 0L },
            scrollReach25 = reach(25),
            scrollReach50 = reach(50),
            scrollReach75 = reach(75),
            scrollReach100 = reach(100),
        )
    }

    private fun visitorOf(row: ContentEventRow): String = row.userId?.let { "u:$it" } ?: "v:${row.visitorId}"
}
