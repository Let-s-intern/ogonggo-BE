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
 * X-Forwarded-For의 마지막 주소를 쓴다. 없으면 연결 주소다.
 * 앞단(ALB)이 붙인 주소는 맨 뒤에 있고, 앞쪽은 클라이언트가 마음대로 넣을 수 있어 믿지 않는다.
 * 앞단이 CloudFront로 바뀌면 `CloudFront-Viewer-Address`를 읽도록 여기만 바꾼다.
 */
private fun HttpServletRequest.clientIp(): String? =
    getHeader(FORWARDED_FOR_HEADER)?.substringAfterLast(',')?.trim()?.takeIf { it.isNotEmpty() } ?: remoteAddr

private const val FORWARDED_FOR_HEADER = "X-Forwarded-For"
