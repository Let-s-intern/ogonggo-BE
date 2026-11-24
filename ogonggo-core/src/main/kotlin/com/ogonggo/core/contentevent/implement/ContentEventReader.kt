package com.ogonggo.core.contentevent.implement

import com.ogonggo.core.contentdailystat.implement.ContentMetricCalculator
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentevent.implement.dto.ContentEventAppendDto
import com.ogonggo.core.contentevent.persistence.ContentEventJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ContentEventReader internal constructor(
    private val contentEventRepository: ContentEventJpaRepository,
) {

    /**
     * 저장할 이벤트 중 조회수에 넣을 상세 조회(VIEW)만 남긴다. 이번 이벤트를 저장하기 전에 불러야 한다.
     *
     * 같은 방문자가 같은 대상을 직전 조회로부터 30분 안에 다시 열었으면 넣지 않는다. 30분이 정확히 지났으면 넣는다.
     * 일별 집계가 조회를 세는 규칙([ContentMetricCalculator])과 같고, 직전 조회가 넣지 않은 조회여도 기준이 된다.
     * 한 요청의 이벤트는 방문자가 같으므로 같은 대상의 둘째 조회부터는 넣지 않는다.
     * 방문자는 회원이면 회원 식별자와 방문자 식별자 어느 쪽으로 남은 조회든 같은 사람으로 본다.
     */
    fun filterCountableViews(events: List<ContentEventAppendDto>, now: LocalDateTime): List<ContentEventAppendDto> {
        val seen = mutableSetOf<Pair<ContentEventTargetType, Long>>()
        val since = now.minus(ContentMetricCalculator.DUPLICATE_VIEW_WINDOW)
        return events.filter { event ->
            val targetType = event.targetType
            val targetId = event.targetId
            event.eventType == ContentEventType.VIEW &&
                targetType != null && targetId != null &&
                seen.add(targetType to targetId) &&
                !hasViewSince(event, targetType, targetId, since)
        }
    }

    private fun hasViewSince(event: ContentEventAppendDto, targetType: ContentEventTargetType, targetId: Long, since: LocalDateTime): Boolean {
        val userId = event.userId
        val count = if (userId != null) {
            contentEventRepository.countViewsByMemberSince(targetType, targetId, userId, event.visitorId, since)
        } else {
            contentEventRepository.countViewsByVisitorSince(targetType, targetId, event.visitorId, since)
        }
        return count > 0
    }
}
