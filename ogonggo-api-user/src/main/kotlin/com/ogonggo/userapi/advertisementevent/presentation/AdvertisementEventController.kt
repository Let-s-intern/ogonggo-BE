package com.ogonggo.userapi.advertisementevent.presentation

import com.ogonggo.userapi.advertisementevent.business.AdvertisementEventService
import com.ogonggo.userapi.advertisementevent.implement.AdvertisementEventIdempotencyStore
import com.ogonggo.userapi.advertisementevent.presentation.request.CreateAdvertisementEventRequest
import com.ogonggo.userapi.response.SuccessResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/advertisement-events")
class AdvertisementEventController(
    private val advertisementEventService: AdvertisementEventService,
    private val advertisementEventIdempotencyStore: AdvertisementEventIdempotencyStore,
) : AdvertisementEventApi {

    @PostMapping
    override fun createEvent(
        @AuthenticationPrincipal userId: Long?,
        @RequestHeader("Idempotency-Key") idempotencyKey: UUID,
        @Valid @RequestBody request: CreateAdvertisementEventRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        if (advertisementEventIdempotencyStore.isCompleted(idempotencyKey)) {
            return SuccessResponse.ok()
        }
        advertisementEventService.record(userId, request, idempotencyKey)
        advertisementEventIdempotencyStore.markCompleted(idempotencyKey)
        return SuccessResponse.ok()
    }
}
