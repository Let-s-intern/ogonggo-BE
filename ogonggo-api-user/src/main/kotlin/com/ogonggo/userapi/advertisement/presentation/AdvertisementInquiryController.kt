package com.ogonggo.userapi.advertisement.presentation

import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryService
import com.ogonggo.userapi.advertisement.presentation.request.CreateAdvertisementInquiryRequest
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AdvertisementInquiryController(
    private val advertisementInquiryService: AdvertisementInquiryService,
) : AdvertisementInquiryApi {

    override fun createInquiry(
        request: CreateAdvertisementInquiryRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        advertisementInquiryService.createInquiry(request.toCommand())
        return SuccessResponse.ok()
    }
}
