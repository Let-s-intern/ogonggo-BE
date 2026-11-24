package com.ogonggo.core.contentdailystat.implement

import com.ogonggo.core.contentdailystat.domain.ContentStatKey
import com.ogonggo.core.contentdailystat.implement.dto.ContentMemberActivityDto
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricCellDto
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricDto
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricScope
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricSheetDto
import com.ogonggo.core.contentdailystat.persistence.ContentDailyStatJpaRepository
import com.ogonggo.core.contentdailystat.persistence.ContentDailyStatQueryRepository
import com.ogonggo.core.contentdailystat.persistence.ContentEventRow
import com.ogonggo.core.contentdailystat.persistence.ContentVisitorKeys
import com.ogonggo.core.contentevent.domain.ContentEvent
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentdailystat.implement.dto.ContentAdReachDto
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ContentMetricReader internal constructor(
    private val contentDailyStatRepository: ContentDailyStatJpaRepository,
    private val contentDailyStatQueryRepository: ContentDailyStatQueryRepository,
) {

    /**
     * [from]~[to](둘 다 포함) 지표를 센다. 일별 집계가 있는 날은 집계를, 그 뒤(오늘, 아직 모으지 않은 전날)는 원본을 읽는다.
     * 어디까지 모았는지는 집계의 마지막 날로 본다. 이벤트가 없어 행이 없는 날은 원본에서 다시 세도 결과가 같다.
     *
     * [detailed]가 false이면 기간 값의 순 방문자·CTA 방문자·회원 수만 SQL `COUNT(DISTINCT)`로 세고 기간 전체의 원본을 읽지 않는다.
     * 이때 스크롤 도달 수는 칸 합이고 날짜별·노출별 순 방문자([ContentMetricSheetDto.dailyUniques])는 비어 있다. 대상 하나의 합계처럼
     * 스크롤 깔때기와 날짜별 값이 필요 없는 곳에 쓴다.
     */
    fun read(scope: ContentMetricScope, from: LocalDate, to: LocalDate, today: LocalDate, detailed: Boolean = true): ContentMetricSheetDto {
        require(!from.isAfter(to)) { "시작일은 종료일보다 늦을 수 없습니다." }
        if (scope.isEmpty) return ContentMetricSheetDto(emptyList(), ContentMetricDto.ZERO, uniqueCountsApproximate = false)

        val rawFrom = rawFrom(from, today)
        val statCells = if (rawFrom.isAfter(from)) {
            contentDailyStatQueryRepository.findStats(scope, from, minOf(to, rawFrom.minusDays(1))).map {
                ContentMetricCellDto(it.statDate, it.targetType, it.targetId, it.exposureId, it.source, it.toMetric())
            }
        } else {
            emptyList()
        }
        val exactUniques = !from.isBefore(ContentEvent.retainedSince(today))
        val detailFrom = if (exactUniques && detailed) from else rawFrom
        val detailStart = detailFrom.atStartOfDay().minus(ContentMetricCalculator.DUPLICATE_VIEW_WINDOW)
        val detailEnd = to.plusDays(1).atStartOfDay()
        val detailRows = if (detailFrom.isAfter(to)) emptyList() else contentDailyStatQueryRepository.findDetailRows(scope, detailStart, detailEnd)
        val visitors = if (detailRows.isEmpty()) ContentVisitorKeys.NONE else contentDailyStatQueryRepository.findVisitorKeys(detailStart, detailEnd)
        val judgeOnly = contentDailyStatQueryRepository.findViewsOutsideScope(scope, detailRows, detailStart, detailEnd)
        val rawCells = if (rawFrom.isAfter(to)) emptyList() else rawCells(scope, detailRows, judgeOnly, visitors, rawFrom, to)
        val cells = statCells + rawCells
        val summed = cells.fold(ContentMetricDto.ZERO) { total, cell -> total + cell.metric }
        if (!exactUniques) return ContentMetricSheetDto(cells, summed, uniqueCountsApproximate = true)
        val fromAt = from.atStartOfDay()
        if (!detailed) {
            val viewers = contentDailyStatQueryRepository.countUniques(scope, ContentEventType.VIEW, fromAt, detailEnd, detailStart, detailEnd)
            val ctaClickers = contentDailyStatQueryRepository.countUniques(scope, ContentEventType.CTA_CLICK, fromAt, detailEnd, detailStart, detailEnd)
            val exact = ContentMetricDto(
                uniqueVisitors = viewers.visitors,
                memberUniqueVisitors = viewers.members,
                ctaUniqueVisitors = ctaClickers.visitors,
                ctaUniqueMembers = ctaClickers.members,
                scrollReach25 = summed.scrollReach25,
                scrollReach50 = summed.scrollReach50,
                scrollReach75 = summed.scrollReach75,
                scrollReach100 = summed.scrollReach100,
            )
            return ContentMetricSheetDto(cells, summed.withUniquesOf(exact), uniqueCountsApproximate = false)
        }
        return ContentMetricSheetDto(
            cells = cells,
            period = summed.withUniquesOf(ContentMetricCalculator.measureAll(detailRows, fromAt, visitors, judgeOnly)),
            uniqueCountsApproximate = false,
            dailyUniques = ContentMetricCalculator.measure(detailRows, fromAt, visitors, judgeOnly) { it.occurredAt.toLocalDate() },
            exposureDailyUniques = ContentMetricCalculator.measure(detailRows, fromAt, visitors, judgeOnly) { it.occurredAt.toLocalDate() to it.exposureId },
        )
    }

    /**
     * [from]~[to] 동안 회원마다 한 활동이다. 회원 단위로 묶어야 해서 원본에서만 센다.
     * 원본 보관 기간(365일)보다 앞인 날은 빠진다.
     */
    fun readMemberActivities(scope: ContentMetricScope, from: LocalDate, to: LocalDate, today: LocalDate): List<ContentMemberActivityDto> {
        val start = maxOf(from, ContentEvent.retainedSince(today))
        if (scope.isEmpty || start.isAfter(to)) return emptyList()
        val toExclusive = to.plusDays(1).atStartOfDay()
        val rowsFrom = start.atStartOfDay().minus(ContentMetricCalculator.DUPLICATE_VIEW_WINDOW)
        val rows = contentDailyStatQueryRepository.findDetailRows(scope, rowsFrom, toExclusive)
        val visitors = contentDailyStatQueryRepository.findVisitorKeys(rowsFrom, toExclusive)
        val judgeOnly = contentDailyStatQueryRepository.findViewsOutsideScope(scope, rows, rowsFrom, toExclusive)
        val activities = ContentMetricCalculator.memberActivities(rows, start.atStartOfDay(), visitors, judgeOnly)
        val adClickers = contentDailyStatQueryRepository.findAdClickMemberIds(scope, start.atStartOfDay(), toExclusive)
        val userIds = activities.keys + adClickers
        val profiles = contentDailyStatQueryRepository.findProfiles(userIds)
        return userIds.map { userId ->
            val activity = activities[userId]
            ContentMemberActivityDto(
                userId = userId,
                wishJob = profiles[userId]?.wishJob,
                grade = profiles[userId]?.grade,
                views = activity?.views ?: 0L,
                viewed = activity?.viewed ?: false,
                ctaClicked = activity?.ctaClicked ?: false,
                adClicked = userId in adClickers,
            )
        }
    }

    /**
     * [from]~[to] 동안 광고를 본 서로 다른 방문자(도달)와 광고를 누른 서로 다른 방문자다. 하루 집계로는 기간 합을 낼 수 없어 원본에서 센다.
     * 원본 보관 기간(365일)보다 앞인 날이 걸리면 정확히 셀 수 없어 null이다.
     */
    fun readAdReach(scope: ContentMetricScope, from: LocalDate, to: LocalDate, today: LocalDate): ContentAdReachDto? {
        if (from.isBefore(ContentEvent.retainedSince(today))) return null
        val fromAt = from.atStartOfDay()
        val toExclusive = to.plusDays(1).atStartOfDay()
        return ContentAdReachDto(
            impressionVisitors = contentDailyStatQueryRepository.countDistinctVisitors(scope, ContentEventType.IMPRESSION, fromAt, toExclusive),
            adClickVisitors = contentDailyStatQueryRepository.countDistinctVisitors(scope, ContentEventType.AD_CLICK, fromAt, toExclusive),
        )
    }

    /**
     * [from]~[to]의 날짜마다 광고를 본 서로 다른 방문자(도달)다. 방문자는 [readAdReach]와 같은 키로 세고 표는 기간 전체로 만든다.
     * 원본 보관 기간(365일)보다 앞인 날은 맵에 없다(정확히 셀 수 없다). 보관 기간 안에서 이벤트가 없는 날은 0이다.
     */
    fun readAdReachByDate(scope: ContentMetricScope, from: LocalDate, to: LocalDate, today: LocalDate): Map<LocalDate, Long> {
        val start = maxOf(from, ContentEvent.retainedSince(today))
        if (start.isAfter(to)) return emptyMap()
        val counted = contentDailyStatQueryRepository.countDistinctVisitorsByDate(
            scope, ContentEventType.IMPRESSION, start.atStartOfDay(), to.plusDays(1).atStartOfDay(),
        )
        return start.datesUntil(to.plusDays(1)).toList().associateWith { counted[it] ?: 0L }
    }

    /** [from]~[to] 공유 수다. 일별 집계에 넣지 않아 원본에서 세며, 원본 보관 기간보다 앞인 날은 빠진다. */
    fun readShares(scope: ContentMetricScope, from: LocalDate, to: LocalDate, today: LocalDate): Long {
        val start = maxOf(from, ContentEvent.retainedSince(today))
        if (start.isAfter(to)) return 0L
        return contentDailyStatQueryRepository.countEvents(scope, ContentEventType.SHARE, start.atStartOfDay(), to.plusDays(1).atStartOfDay())
    }

    /**
     * 원본에서만 세는 값([readMemberActivities], [readShares])이 [from]부터 다 세지 못하는지다.
     * 원본 보관 기간(365일)보다 앞인 날이 걸리면 true이고 그 앞날은 값에서 빠진다.
     */
    fun isBeyondRawRetention(from: LocalDate, today: LocalDate): Boolean = from.isBefore(ContentEvent.retainedSince(today))

    /** 이벤트를 모으기 시작한 날이다. 아직 없으면 [today]다. */
    fun readCollectedSince(today: LocalDate): LocalDate =
        listOfNotNull(
            contentDailyStatRepository.findFirstStatDate(),
            contentDailyStatQueryRepository.findFirstOccurredAt()?.toLocalDate(),
        ).minOrNull() ?: today

    private fun rawFrom(from: LocalDate, today: LocalDate): LocalDate {
        val afterLastStat = contentDailyStatRepository.findLastStatDate()?.plusDays(1) ?: from
        return maxOf(from, minOf(afterLastStat, today))
    }

    private fun rawCells(
        scope: ContentMetricScope,
        detailRows: List<ContentEventRow>,
        judgeOnly: List<ContentEventRow>,
        visitors: ContentVisitorKeys,
        rawFrom: LocalDate,
        to: LocalDate,
    ): List<ContentMetricCellDto> {
        val detail = ContentMetricCalculator.measure(detailRows, rawFrom.atStartOfDay(), visitors, judgeOnly) {
            it.occurredAt.toLocalDate() to ContentMetricCalculator.statKeyOf(it)
        }
        val ads = rawFrom.datesUntil(to.plusDays(1)).toList().flatMap { date ->
            ContentMetricCalculator.adMetricsByKey(
                contentDailyStatQueryRepository.countAdEvents(scope, date.atStartOfDay(), date.plusDays(1).atStartOfDay()),
                contentDailyStatQueryRepository.countUniqueImpressions(scope, date.atStartOfDay(), date.plusDays(1).atStartOfDay()),
            ).map { (key, metric) -> (date to key) to metric }
        }.toMap()
        return (detail.keys + ads.keys).map { dateKey ->
            val (date, key: ContentStatKey) = dateKey
            val metric = (detail[dateKey] ?: ContentMetricDto.ZERO) + (ads[dateKey] ?: ContentMetricDto.ZERO)
            ContentMetricCellDto(date, key.targetType, key.targetId, key.exposureId, key.source, metric)
        }
    }
}
