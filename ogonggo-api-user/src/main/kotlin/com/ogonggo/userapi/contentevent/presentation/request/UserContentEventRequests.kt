package com.ogonggo.userapi.contentevent.presentation.request

import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.userapi.contentevent.business.CollectContentEventCommand
import com.ogonggo.userapi.contentevent.business.CollectContentEventsCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class CreateContentEventsRequest(
    @Schema(description = "브라우저의 익명 방문자 식별자(UUID). 쿠키 ogg_vid 값")
    @field:NotBlank
    @field:Size(max = 36)
    val visitorId: String,
    @field:Size(min = 1, max = MAX_EVENTS)
    @field:Valid
    val events: List<ContentEventRequest>,
) {
    fun toCommand(clientIp: String?, userAgent: String?): CollectContentEventsCommand = CollectContentEventsCommand(
        visitorId = visitorId,
        events = events.map(ContentEventRequest::toCommand),
        clientIp = clientIp,
        userAgent = userAgent,
    )

    companion object {
        const val MAX_EVENTS = 20
    }
}

data class ContentEventRequest(
    val type: ContentEventType,
    @Schema(description = "VIEW·CTA_CLICK·SCROLL은 필수. 대상 없는 배너의 IMPRESSION·AD_CLICK은 null")
    val targetType: ContentEventTargetType?,
    @field:Positive
    val targetId: Long?,
    @Schema(description = "광고 노출 식별자. IMPRESSION·AD_CLICK은 필수이고, 상세 이벤트는 광고로 들어왔을 때만 보낸다")
    @field:Positive
    val exposureId: Long?,
    val source: ContentEventSource,
    @Schema(description = "SCROLL만. 가장 깊이 내린 위치(%)")
    @field:Min(0)
    @field:Max(100)
    val scrollPercent: Int?,
    @Schema(description = "SCROLL만. 화면이 보인 시간(밀리초)")
    @field:PositiveOrZero
    val durationMs: Long?,
) {
    fun toCommand(): CollectContentEventCommand = CollectContentEventCommand(
        type = type,
        targetType = targetType,
        targetId = targetId,
        exposureId = exposureId,
        source = source,
        scrollPercent = scrollPercent,
        durationMs = durationMs,
    )
}
