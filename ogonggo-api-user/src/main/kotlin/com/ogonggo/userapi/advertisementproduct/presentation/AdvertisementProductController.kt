package com.ogonggo.userapi.advertisementproduct.presentation

import com.ogonggo.userapi.advertisementproduct.business.AdvertisementProductService
import com.ogonggo.userapi.advertisementproduct.presentation.response.AdvertisementProductResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/advertisement-products")
class AdvertisementProductController(
    private val advertisementProductService: AdvertisementProductService,
) : AdvertisementProductApi {

    @GetMapping
    override fun getProducts(): ResponseEntity<SuccessResponse<List<AdvertisementProductResponse>>> =
        SuccessResponse.ok(advertisementProductService.getProducts().map(AdvertisementProductResponse::from))
}
