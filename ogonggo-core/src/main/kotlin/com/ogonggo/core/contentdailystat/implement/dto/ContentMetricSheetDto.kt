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
 */
data class ContentMetricSheetDto(
    val cells: List<ContentMetricCellDto>,
    val period: ContentMetricDto,
    val uniqueCountsApproximate: Boolean,
)
