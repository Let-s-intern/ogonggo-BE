package com.ogonggo.core.contentdailystat.implement.dto

import com.ogonggo.core.contentevent.domain.ContentEventTargetType

/** 지표를 셀 범위다. 광고 노출 몇 개(상품 몫) 또는 대상 하나(대상 전체)다. 노출이 비어 있으면 아무것도 세지 않는다. */
sealed interface ContentMetricScope {
    data class Exposures(val exposureIds: Set<Long>) : ContentMetricScope
    data class Target(val targetType: ContentEventTargetType, val targetId: Long) : ContentMetricScope
    data object All : ContentMetricScope

    val isEmpty: Boolean
        get() = this is Exposures && exposureIds.isEmpty()
}
