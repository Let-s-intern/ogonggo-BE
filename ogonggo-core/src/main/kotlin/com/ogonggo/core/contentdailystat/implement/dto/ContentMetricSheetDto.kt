package com.ogonggo.core.contentdailystat.implement.dto

import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import java.time.LocalDate

/** 하루·대상·노출·유입 경로 한 칸의 지표다. 전날까지는 일별 집계, 그 뒤는 원본에서 센 값이다. */
data class ContentMetricCellDto(
    val date: LocalDate,
    val targetType: ContentEventTargetType?,
    val targetId: Long?,
    val exposureId: Long?,
    val source: ContentEventSource,
    val metric: ContentMetricDto,
)

/**
 * 한 범위의 기간 지표다. [cells]를 날짜·노출·유입 경로로 묶어 쓰고, 기간 전체 숫자는 [period]를 쓴다.
 *
 * [period]의 순 방문자 수와 스크롤 도달 수는 원본에서 기간 전체로 센 값이다. 기간이 원본 보관 기간(365일)보다 앞이면
 * 날마다 센 값을 더해 대신하고 [uniqueCountsApproximate]가 true다. 그러면 여러 날 온 방문자가 여러 번 셀 수 있다.
 *
 * [cells]의 방문자 수를 날짜별로 더하면 같은 사람이 두 노출·유입 경로로 들어온 날 두 번 들어간다. 그래서 원본에서 센 날짜별 값
 * ([dailyUniques])과 날짜·노출별 값([exposureDailyUniques], 노출 없는 이벤트는 노출 키가 null)을 따로 준다.
 * 방문자 수로 세는 필드만 의미가 있으며 [ContentMetricDto.withUniquesOf]로 칸 합에 덮어 쓴다.
 * [uniqueCountsApproximate]가 true이면 원본을 읽지 않으므로 둘 다 비어 있고 칸 합을 그대로 쓴다.
 */
data class ContentMetricSheetDto(
    val cells: List<ContentMetricCellDto>,
    val period: ContentMetricDto,
    val uniqueCountsApproximate: Boolean,
    val dailyUniques: Map<LocalDate, ContentMetricDto> = emptyMap(),
    val exposureDailyUniques: Map<Pair<LocalDate, Long?>, ContentMetricDto> = emptyMap(),
)
