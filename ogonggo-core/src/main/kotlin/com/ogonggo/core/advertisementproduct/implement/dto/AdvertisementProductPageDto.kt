package com.ogonggo.core.advertisementproduct.implement.dto

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct

/** 광고 상품 목록의 페이지 정보다. 페이지 번호는 0부터 시작한다. */
data class AdvertisementProductPageDto(
    val items: List<AdvertisementProduct>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
