package com.ogonggo.userapi.advertisement.presentation

import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryService
import com.ogonggo.userapi.advertisement.presentation.request.CreateAdvertisementInquiryRequest
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/advertisement-inquiries")
class AdvertisementInquiryController(
    private val advertisementInquiryService: AdvertisementInquiryService,
) : AdvertisementInquiryApi {

    @PostMapping
    override fun createInquiry(
        @RequestBody request: CreateAdvertisementInquiryRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        advertisementInquiryService.createInquiry(request.toCommand())
        return SuccessResponse.ok()
    }
}
