package com.ogonggo.userapi.contentevent.business

import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentevent.implement.ContentEventAppender
import com.ogonggo.core.contentevent.implement.ContentEventValidator
import com.ogonggo.core.contentevent.implement.dto.ContentEventAppendDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class UserContentEventService(
    private val contentEventValidator: ContentEventValidator,
    private val contentEventAppender: ContentEventAppender,
    private val clock: Clock,
) {

    /**
     * 브라우저가 묶어 보낸 이벤트를 저장한다. 비로그인이면 userId가 없다.
     * 저장할 수 없는 건은 그 건만 버리고, 시각은 받은 시각 하나로 적는다.
     */
    @Transactional
    fun collect(userId: Long?, command: CollectContentEventsCommand) {
        val now = LocalDateTime.now(clock)
        val events = command.events.map {
            ContentEventAppendDto(
                eventType = it.type,
                targetType = it.targetType,
                targetId = it.targetId,
                exposureId = it.exposureId,
                userId = userId,
                visitorId = command.visitorId,
                source = it.source,
                scrollPercent = it.scrollPercent,
                durationMs = it.durationMs,
            )
        }
        contentEventAppender.appendAll(contentEventValidator.filterStorable(events, now), now)
    }
}

data class CollectContentEventsCommand(
    val visitorId: String,
    val events: List<CollectContentEventCommand>,
)

data class CollectContentEventCommand(
    val type: ContentEventType,
    val targetType: ContentEventTargetType?,
    val targetId: Long?,
    val exposureId: Long?,
    val source: ContentEventSource,
    val scrollPercent: Int?,
    val durationMs: Long?,
)
