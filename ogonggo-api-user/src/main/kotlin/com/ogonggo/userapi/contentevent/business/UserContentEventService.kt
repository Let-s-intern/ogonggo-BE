package com.ogonggo.userapi.contentevent.business

import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentevent.implement.ContentEventAppender
import com.ogonggo.core.contentevent.implement.ContentEventReader
import com.ogonggo.core.contentevent.implement.ContentEventValidator
import com.ogonggo.core.contentevent.implement.dto.ContentEventAppendDto
import com.ogonggo.userapi.bootcamp.business.BootcampViewedEvent
import com.ogonggo.userapi.concern.business.ConcernViewedEvent
import com.ogonggo.userapi.contentevent.implement.ContentEventRequestGuard
import com.ogonggo.userapi.job.business.JobViewedEvent
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostViewedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class UserContentEventService(
    private val contentEventValidator: ContentEventValidator,
    private val contentEventAppender: ContentEventAppender,
    private val contentEventReader: ContentEventReader,
    private val contentEventRequestGuard: ContentEventRequestGuard,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {

    /**
     * 브라우저가 묶어 보낸 이벤트를 저장한다. 비로그인이면 userId가 없다.
     * 저장할 수 없는 건은 그 건만 버리고, 시각은 받은 시각 하나로 적는다.
     * 봇이거나 방문자·IP의 분당 요청 상한을 넘으면 아무것도 저장하지 않는다.
     *
     * 저장하는 상세 조회(VIEW)는 대상의 누적 조회수도 올린다. 같은 방문자가 같은 대상을 30분 안에 다시 연 것은 올리지 않는다.
     * 중복은 이번 이벤트를 저장하기 전에 가르고, 지표 갱신은 조회 사실만 발행해 지표 실행기에 맡긴다.
     */
    @Transactional
    fun collect(userId: Long?, command: CollectContentEventsCommand) {
        if (!contentEventRequestGuard.isAllowed(command.visitorId, command.clientIp, command.userAgent)) return
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
        val storable = contentEventValidator.filterStorable(events, now)
        val countable = contentEventReader.filterCountableViews(storable, now)
        contentEventAppender.appendAll(storable, now)
        countable.forEach(::publishViewed)
    }

    private fun publishViewed(event: ContentEventAppendDto) {
        val targetId = checkNotNull(event.targetId) { "상세 조회에는 대상이 있어야 합니다." }
        eventPublisher.publishEvent(
            when (checkNotNull(event.targetType) { "상세 조회에는 대상 종류가 있어야 합니다." }) {
                ContentEventTargetType.JOB -> JobViewedEvent(targetId)
                ContentEventTargetType.BOOTCAMP -> BootcampViewedEvent(targetId)
                ContentEventTargetType.CONCERN -> ConcernViewedEvent(targetId)
                ContentEventTargetType.RECRUITMENT_POST -> RecruitmentPostViewedEvent(targetId)
            },
        )
    }
}

data class CollectContentEventsCommand(
    val visitorId: String,
    val events: List<CollectContentEventCommand>,
    val clientIp: String?,
    val userAgent: String?,
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
