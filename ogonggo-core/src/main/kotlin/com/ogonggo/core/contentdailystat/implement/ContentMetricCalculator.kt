package com.ogonggo.core.contentdailystat.implement

import com.ogonggo.core.contentdailystat.domain.ContentStatKey
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricDto
import com.ogonggo.core.contentdailystat.persistence.ContentAdEventCount
import com.ogonggo.core.contentdailystat.persistence.ContentEventRow
import com.ogonggo.core.contentdailystat.persistence.ContentVisitorKeys
import com.ogonggo.core.contentevent.domain.ContentEventType
import java.time.Duration
import java.time.LocalDateTime
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 원본 이벤트로 지표를 센다. 일별 집계와 리포트의 오늘 숫자가 같은 규칙으로 세도록 한 곳에 둔다.
 *
 * - 방문자는 [ContentVisitorKeys] 규칙으로 가른다. 회원이면 회원 식별자이고, 비회원이어도 같은 브라우저가 계산 범위 안에서 로그인한 적이 있으면 그 회원이다.
 * - 같은 방문자가 같은 대상을 직전 조회로부터 30분 안에 다시 열면 세지 않는다. 직전 조회가 세지 않은 조회여도 기준이 된다.
 *   30분이 정확히 지난 조회는 센다. 하루 첫 30분은 전날 끝 30분의 조회를 보고 가른다([from] 앞의 행은 이 판단에만 쓴다).
 * - 판단에만 쓰는 조회([measure]의 `judgeOnly`)가 있다. 센 범위 밖의 같은 방문자·대상 조회(목록에서 연 조회 등)도 직전 조회로 보지만 세지는 않는다.
 * - 30분 안 재조회가 다른 유입 경로로 들어와도 세지 않는다. 센 조회는 그 조회의 유입 경로·노출로 들어간다.
 */
internal object ContentMetricCalculator {

    val DUPLICATE_VIEW_WINDOW: Duration = Duration.ofMinutes(30)

    fun <K> measure(
        rows: List<ContentEventRow>,
        from: LocalDateTime,
        visitors: ContentVisitorKeys,
        judgeOnly: List<ContentEventRow> = emptyList(),
        keyOf: (ContentEventRow) -> K,
    ): Map<K, ContentMetricDto> {
        val countedViews = countedViews(rows, judgeOnly, visitors)
        val inRange = rows.filter { !it.occurredAt.isBefore(from) }
        return inRange.groupBy(keyOf).mapValues { (_, keyRows) -> measureRows(keyRows, countedViews, visitors) }
    }

    fun measureAll(
        rows: List<ContentEventRow>,
        from: LocalDateTime,
        visitors: ContentVisitorKeys,
        judgeOnly: List<ContentEventRow> = emptyList(),
    ): ContentMetricDto = measure(rows, from, visitors, judgeOnly) { Unit }[Unit] ?: ContentMetricDto.ZERO

    fun statKeyOf(row: ContentEventRow) = ContentStatKey(row.targetType, row.targetId, row.exposureId, row.source)

    /** 광고 노출·클릭 수와 순 노출수를 집계 행 키마다 묶는다. [uniqueImpressions]는 하루 범위로 센 값이어야 한다. */
    fun adMetricsByKey(counts: List<ContentAdEventCount>, uniqueImpressions: List<ContentAdEventCount>): Map<ContentStatKey, ContentMetricDto> {
        val unique = uniqueImpressions.groupBy(::keyOf).mapValues { (_, keyCounts) -> keyCounts.sumOf { it.count } }
        val totals = counts.groupBy(::keyOf)
        return (totals.keys + unique.keys).associateWith { key ->
            val keyCounts = totals[key].orEmpty()
            ContentMetricDto(
                impressions = keyCounts.filter { it.eventType == ContentEventType.IMPRESSION }.sumOf { it.count },
                uniqueImpressions = unique[key] ?: 0L,
                adClicks = keyCounts.filter { it.eventType == ContentEventType.AD_CLICK }.sumOf { it.count },
            )
        }
    }

    private fun keyOf(count: ContentAdEventCount) = ContentStatKey(count.targetType, count.targetId, count.exposureId, count.source)

    /**
     * 회원마다 [from] 뒤의 상세 활동이다. 조회수는 30분 안 재조회를 뺀 값이다. 비회원은 넣지 않는다.
     */
    fun memberActivities(
        rows: List<ContentEventRow>,
        from: LocalDateTime,
        visitors: ContentVisitorKeys,
        judgeOnly: List<ContentEventRow> = emptyList(),
    ): Map<Long, MemberActivity> {
        val countedViews = countedViews(rows, judgeOnly, visitors)
        return rows.filter { it.userId != null && !it.occurredAt.isBefore(from) }
            .groupBy { it.userId!! }
            .mapValues { (_, memberRows) ->
                MemberActivity(
                    views = memberRows.count { it.eventType == ContentEventType.VIEW && it in countedViews }.toLong(),
                    viewed = memberRows.any { it.eventType == ContentEventType.VIEW },
                    ctaClicked = memberRows.any { it.eventType == ContentEventType.CTA_CLICK },
                )
            }
    }

    data class MemberActivity(val views: Long, val viewed: Boolean, val ctaClicked: Boolean)

    private fun countedViews(rows: List<ContentEventRow>, judgeOnly: List<ContentEventRow>, visitors: ContentVisitorKeys): Set<ContentEventRow> {
        val lastViewAt = mutableMapOf<Triple<String, Any?, Long?>, LocalDateTime>()
        // 같은 순간에 같은 값으로 두 번 온 조회를 가르려고 값이 아니라 객체로 기억한다.
        val counted: MutableSet<ContentEventRow> = Collections.newSetFromMap(IdentityHashMap())
        (rows + judgeOnly).filter { it.eventType == ContentEventType.VIEW }
            .sortedBy { it.occurredAt }
            .forEach { row ->
                val key = Triple(visitors.of(row), row.targetType, row.targetId)
                val previous = lastViewAt[key]
                if (previous == null || !row.occurredAt.isBefore(previous.plus(DUPLICATE_VIEW_WINDOW))) {
                    counted += row
                }
                lastViewAt[key] = row.occurredAt
            }
        return counted
    }

    private fun measureRows(rows: List<ContentEventRow>, countedViews: Set<ContentEventRow>, visitors: ContentVisitorKeys): ContentMetricDto {
        val views = rows.filter { it.eventType == ContentEventType.VIEW }
        val counted = views.filter { it in countedViews }
        val ctaClicks = rows.filter { it.eventType == ContentEventType.CTA_CLICK }
        val scrolls = rows.filter { it.eventType == ContentEventType.SCROLL }
        val viewVisitors = views.map(visitors::of).toSet()
        val maxScrollByVisitor = scrolls.groupBy(visitors::of)
            .filterKeys { it in viewVisitors }
            .mapValues { (_, visitorScrolls) -> visitorScrolls.maxOf { it.scrollPercent ?: 0 } }
        fun reach(threshold: Int) = maxScrollByVisitor.values.count { it >= threshold }.toLong()
        return ContentMetricDto(
            views = counted.size.toLong(),
            uniqueVisitors = viewVisitors.size.toLong(),
            memberViews = counted.count { it.userId != null }.toLong(),
            memberUniqueVisitors = views.mapNotNull { it.userId }.toSet().size.toLong(),
            ctaClicks = ctaClicks.size.toLong(),
            ctaUniqueVisitors = ctaClicks.map(visitors::of).toSet().size.toLong(),
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
}
