package com.ogonggo.core.advertisementexposure.implement.dto

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureContent
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposurePeriod
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import org.springframework.data.domain.Page

/** A-13에서 상품 활성 상태를 잠금으로 확인한 뒤 수정할 대상이다. */
data class AdvertisementExposureUpdateTargetDto(
    val exposure: AdvertisementExposure,
    val product: AdvertisementProduct,
)

/** 노출을 만들 때 넣는 값이다. 순서는 그 자리의 맨 뒤로 정한다. */
data class AdvertisementExposureAppendDto(
    val productId: Long,
    val contractId: Long?,
    val content: AdvertisementExposureContent,
    val adLabel: Boolean,
    val period: AdvertisementExposurePeriod,
    val exposed: Boolean,
)

/** 노출을 고칠 때 넣는 값이다. 자리(상품)·순서·켜짐은 따로 바꾼다. */
data class AdvertisementExposureUpdateDto(
    val contractId: Long?,
    val content: AdvertisementExposureContent,
    val adLabel: Boolean,
    val period: AdvertisementExposurePeriod,
)

/** 광고 노출 목록의 페이지 정보다. 페이지 번호는 0부터 시작한다. */
data class AdvertisementExposurePageDto(
    val items: List<AdvertisementExposure>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(page: Page<AdvertisementExposure>) = AdvertisementExposurePageDto(
            items = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
