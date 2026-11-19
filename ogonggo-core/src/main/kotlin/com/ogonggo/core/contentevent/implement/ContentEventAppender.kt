package com.ogonggo.core.contentevent.implement

import com.ogonggo.core.contentevent.domain.ContentEvent
import com.ogonggo.core.contentevent.implement.dto.ContentEventAppendDto
import com.ogonggo.core.contentevent.persistence.ContentEventJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ContentEventAppender internal constructor(
    private val contentEventRepository: ContentEventJpaRepository,
) {

    /** 한 요청에 묶여 온 이벤트를 같은 시각으로 저장한다. */
    fun appendAll(events: List<ContentEventAppendDto>, occurredAt: LocalDateTime) {
        if (events.isEmpty()) return
        contentEventRepository.saveAll(
            events.map {
                ContentEvent(
                    eventType = it.eventType,
                    targetType = it.targetType,
                    targetId = it.targetId,
                    exposureId = it.exposureId,
                    userId = it.userId,
                    visitorId = it.visitorId,
                    source = it.source,
                    scrollPercent = it.scrollPercent,
                    durationMs = it.durationMs,
                    occurredAt = occurredAt,
                )
            },
        )
    }
}
