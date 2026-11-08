package com.ogonggo.core.advertisementexposure.implement.dto

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureContent
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposurePeriod
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct

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
