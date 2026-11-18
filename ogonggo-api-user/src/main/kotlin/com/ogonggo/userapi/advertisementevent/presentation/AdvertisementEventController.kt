package com.ogonggo.userapi.advertisementevent.presentation

import com.ogonggo.userapi.advertisementevent.business.AdvertisementEventService
import com.ogonggo.userapi.advertisementevent.presentation.request.CreateAdvertisementEventRequest
import com.ogonggo.userapi.response.SuccessResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/advertisement-events")
class AdvertisementEventController(
    private val advertisementEventService: AdvertisementEventService,
) : AdvertisementEventApi {

    @PostMapping
    override fun createEvent(
        @AuthenticationPrincipal userId: Long?,
        @Valid @RequestBody request: CreateAdvertisementEventRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        advertisementEventService.record(userId, request)
        return SuccessResponse.ok()
    }
}
