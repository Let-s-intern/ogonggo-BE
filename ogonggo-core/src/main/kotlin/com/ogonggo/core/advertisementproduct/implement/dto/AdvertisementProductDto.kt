package com.ogonggo.core.advertisementproduct.implement.dto

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition

/** 광고 상품을 등록하거나 고칠 때 넣는 값이다. */
data class AdvertisementProductSaveDto(
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val name: String,
    val price: Int?,
)
