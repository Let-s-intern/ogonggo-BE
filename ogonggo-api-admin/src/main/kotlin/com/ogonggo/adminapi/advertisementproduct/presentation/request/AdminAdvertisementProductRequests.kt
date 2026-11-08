package com.ogonggo.adminapi.advertisementproduct.presentation.request

import com.ogonggo.adminapi.advertisementproduct.business.AdminAdvertisementProductSaveCommand
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateAdminAdvertisementProductRequest(
    val category: AdvertisementProductCategory,
    @field:Schema(description = "오공고 노출 자리. 정해진 자리가 없는 상품이면 null입니다.", nullable = true)
    val position: AdvertisementProductPosition? = null,
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
    @field:Min(0)
    @field:Schema(description = "소개 가격(원). null은 견적 문의이며 0원과 구분합니다.", nullable = true)
    val price: Int? = null,
) {
    fun toCommand(): AdminAdvertisementProductSaveCommand = AdminAdvertisementProductSaveCommand(
        category = category,
        position = position,
        name = name,
        price = price,
    )
}

data class ReplaceAdminAdvertisementProductRequest(
    val category: AdvertisementProductCategory,
    @field:Schema(description = "오공고 노출 자리. 정해진 자리가 없는 상품이면 null입니다.", nullable = true)
    val position: AdvertisementProductPosition? = null,
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
    @field:Min(0)
    @field:Schema(description = "소개 가격(원). null은 견적 문의이며 0원과 구분합니다.", nullable = true)
    val price: Int? = null,
) {
    fun toCommand(): AdminAdvertisementProductSaveCommand = AdminAdvertisementProductSaveCommand(
        category = category,
        position = position,
        name = name,
        price = price,
    )
}
