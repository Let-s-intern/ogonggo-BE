package com.ogonggo.adminapi.advertisementproduct.presentation.response

import com.ogonggo.adminapi.advertisementproduct.business.AdminAdvertisementProductResult
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminAdvertisementProductResponse(
    val id: Long,
    val category: AdvertisementProductCategory,
    @field:Schema(description = "오공고 노출 자리. 정해진 자리가 없는 상품이면 null입니다.", nullable = true)
    val position: AdvertisementProductPosition?,
    val name: String,
    @field:Schema(description = "소개 가격(원). null은 견적 문의이며 0원과 구분합니다.", nullable = true)
    val price: Int?,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminAdvertisementProductResult): AdminAdvertisementProductResponse = AdminAdvertisementProductResponse(
            id = result.id,
            category = result.category,
            position = result.position,
            name = result.name,
            price = result.price,
            registeredAt = result.registeredAt,
            updatedAt = result.updatedAt,
        )
    }
}

data class CreateAdminAdvertisementProductResponse(val id: Long)
