package com.ogonggo.userapi.contentevent.presentation

import com.ogonggo.userapi.contentevent.business.UserContentEventService
import com.ogonggo.userapi.contentevent.presentation.request.CreateContentEventsRequest
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
    ): ResponseEntity<Void> {
        userContentEventService.collect(userId, request.toCommand())
        return ResponseEntity.accepted().build()
    }
}
