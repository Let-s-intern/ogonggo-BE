package com.ogonggo.userapi.advertisementexposure.presentation

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.userapi.advertisementexposure.business.AdvertisementExposureService
import com.ogonggo.userapi.advertisementexposure.presentation.response.AdvertisementExposureResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/advertisement-exposures")
class AdvertisementExposureController(
    private val advertisementExposureService: AdvertisementExposureService,
) : AdvertisementExposureApi {

    @GetMapping
    override fun getShown(
        @RequestParam("position") position: AdvertisementProductPosition,
    ): ResponseEntity<SuccessResponse<List<AdvertisementExposureResponse>>> =
        SuccessResponse.ok(advertisementExposureService.getShown(position).map(AdvertisementExposureResponse::from))
}
