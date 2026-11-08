package com.ogonggo.adminapi.advertisementproduct.presentation

import com.ogonggo.adminapi.advertisementproduct.business.AdminAdvertisementProductService
import com.ogonggo.adminapi.advertisementproduct.presentation.request.CreateAdminAdvertisementProductRequest
import com.ogonggo.adminapi.advertisementproduct.presentation.request.ReplaceAdminAdvertisementProductRequest
import com.ogonggo.adminapi.advertisementproduct.presentation.response.AdminAdvertisementProductResponse
import com.ogonggo.adminapi.advertisementproduct.presentation.response.CreateAdminAdvertisementProductResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/advertisement-products")
class AdminAdvertisementProductController(
    private val adminAdvertisementProductService: AdminAdvertisementProductService,
) : AdminAdvertisementProductApi {

    @GetMapping
    override fun getProducts(): ResponseEntity<SuccessResponse<List<AdminAdvertisementProductResponse>>> =
        SuccessResponse.ok(adminAdvertisementProductService.getProducts().map(AdminAdvertisementProductResponse::from))

    @PostMapping
    override fun createProduct(
        @RequestBody request: CreateAdminAdvertisementProductRequest,
    ): ResponseEntity<SuccessResponse<CreateAdminAdvertisementProductResponse>> =
        SuccessResponse.created(
            CreateAdminAdvertisementProductResponse(
                adminAdvertisementProductService.createProduct(request.toCommand()),
            ),
        )

    @PutMapping("/{productId}")
    override fun replaceProduct(
        @PathVariable("productId") productId: Long,
        @RequestBody request: ReplaceAdminAdvertisementProductRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminAdvertisementProductService.replaceProduct(productId, request.toCommand())
        return SuccessResponse.ok()
    }

    @DeleteMapping("/{productId}")
    override fun deleteProduct(
        @PathVariable("productId") productId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminAdvertisementProductService.deleteProduct(productId)
        return SuccessResponse.ok()
    }
}
