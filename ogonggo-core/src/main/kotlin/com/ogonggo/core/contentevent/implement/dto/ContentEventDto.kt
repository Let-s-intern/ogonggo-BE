package com.ogonggo.core.contentevent.implement.dto

import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType

/** 저장할 이벤트 한 건이다. 시각은 Appender가 받은 기준 시각을 쓴다. */
data class ContentEventAppendDto(
    val eventType: ContentEventType,
    val targetType: ContentEventTargetType?,
    val targetId: Long?,
    val exposureId: Long?,
    val userId: Long?,
    val visitorId: String,
    val source: ContentEventSource,
    val scrollPercent: Int? = null,
    val durationMs: Long? = null,
)
