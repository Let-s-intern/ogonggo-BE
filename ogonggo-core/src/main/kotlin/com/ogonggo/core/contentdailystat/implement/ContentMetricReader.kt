package com.ogonggo.core.contentdailystat.implement

import com.ogonggo.core.contentdailystat.domain.ContentStatKey
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricCellDto
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricDto
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricScope
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricSheetDto
import com.ogonggo.core.contentdailystat.persistence.ContentDailyStatJpaRepository
import com.ogonggo.core.contentdailystat.persistence.ContentDailyStatQueryRepository
import com.ogonggo.core.contentdailystat.persistence.ContentEventRow
import com.ogonggo.core.contentevent.domain.ContentEvent
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
     */
    fun read(scope: ContentMetricScope, from: LocalDate, to: LocalDate, today: LocalDate): ContentMetricSheetDto {
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
        val detailFrom = if (exactUniques) from else rawFrom
        val detailRows = if (detailFrom.isAfter(to)) {
            emptyList()
        } else {
            contentDailyStatQueryRepository.findDetailRows(
                scope,
                detailFrom.atStartOfDay().minus(ContentMetricCalculator.DUPLICATE_VIEW_WINDOW),
                to.plusDays(1).atStartOfDay(),
            )
        }
        val rawCells = if (rawFrom.isAfter(to)) emptyList() else rawCells(scope, detailRows, rawFrom, to)
        val cells = statCells + rawCells
        val summed = cells.fold(ContentMetricDto.ZERO) { total, cell -> total + cell.metric }
        val period = if (exactUniques) withUniques(summed, ContentMetricCalculator.measureAll(detailRows, from.atStartOfDay())) else summed
        return ContentMetricSheetDto(cells, period, uniqueCountsApproximate = !exactUniques)
    }

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
        rawFrom: LocalDate,
        to: LocalDate,
    ): List<ContentMetricCellDto> {
        val detail = ContentMetricCalculator.measure(detailRows, rawFrom.atStartOfDay()) {
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

    private fun withUniques(summed: ContentMetricDto, exact: ContentMetricDto) = summed.copy(
        uniqueVisitors = exact.uniqueVisitors,
        memberUniqueVisitors = exact.memberUniqueVisitors,
        ctaUniqueVisitors = exact.ctaUniqueVisitors,
        ctaUniqueMembers = exact.ctaUniqueMembers,
        scrollReach25 = exact.scrollReach25,
        scrollReach50 = exact.scrollReach50,
        scrollReach75 = exact.scrollReach75,
        scrollReach100 = exact.scrollReach100,
    )
}
