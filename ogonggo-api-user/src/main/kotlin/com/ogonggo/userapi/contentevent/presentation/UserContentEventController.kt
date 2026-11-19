package com.ogonggo.userapi.contentevent.presentation

import com.ogonggo.userapi.contentevent.business.UserContentEventService
import com.ogonggo.userapi.contentevent.presentation.request.CreateContentEventsRequest
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/events")
class UserContentEventController(
    private val userContentEventService: UserContentEventService,
) : UserContentEventApi {

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE])
    override fun createContentEvents(
        @AuthenticationPrincipal userId: Long?,
        @RequestBody request: CreateContentEventsRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Void> {
        userContentEventService.collect(
            userId,
            request.toCommand(clientIp = httpRequest.clientIp(), userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT)),
        )
        return ResponseEntity.accepted().build()
    }
}

/**
 * 앞단 프록시가 붙인 X-Forwarded-For의 첫 주소를 쓴다. 없으면 연결 주소다.
 * 첫 주소는 클라이언트가 바꿀 수 있지만 요청 수 제한에만 쓰므로 받아들인다.
 */
private fun HttpServletRequest.clientIp(): String? =
    getHeader(FORWARDED_FOR_HEADER)?.substringBefore(',')?.trim()?.takeIf { it.isNotEmpty() } ?: remoteAddr

private const val FORWARDED_FOR_HEADER = "X-Forwarded-For"
