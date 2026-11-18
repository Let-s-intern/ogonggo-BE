package com.ogonggo.userapi.advertisementevent.presentation.request

import com.ogonggo.core.advertisementevent.domain.AdvertisementEventType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateAdvertisementEventRequest(
    @field:Positive
    val exposureId: Long,
    val eventType: AdvertisementEventType,
    @field:NotBlank
    @field:Size(max = 128)
    val anonymousId: String,
)
