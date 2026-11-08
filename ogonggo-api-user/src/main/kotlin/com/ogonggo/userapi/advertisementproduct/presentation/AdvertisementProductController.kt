package com.ogonggo.userapi.advertisementproduct.presentation

import com.ogonggo.userapi.advertisementproduct.business.AdvertisementProductService
import com.ogonggo.userapi.advertisementproduct.presentation.response.AdvertisementProductResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/advertisement-products")
class AdvertisementProductController(
    private val advertisementProductService: AdvertisementProductService,
) : AdvertisementProductApi {

    @GetMapping
    override fun getProducts(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<AdvertisementProductResponse>>> {
        val result = advertisementProductService.getProducts(page - 1, size)
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(AdvertisementProductResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }
}
