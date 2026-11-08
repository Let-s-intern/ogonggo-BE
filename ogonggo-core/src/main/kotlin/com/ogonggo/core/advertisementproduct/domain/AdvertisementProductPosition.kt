package com.ogonggo.core.advertisementproduct.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 광고가 노출되는 오공고 화면의 자리다. 프론트가 자기 자리의 광고를 찾는 고정 이름이다.
 *
 * 광고 상품은 어드민이 추가하는 행이라 식별자가 환경마다 달라 프론트가 붙잡을 수 없다.
 * 새 자리는 프론트가 그 자리를 그려야 생기므로 값을 더하는 배포와 함께 늘린다.
 * 광고 공고 카테고리의 자리는 아직 정하지 않았다(확인 필요).
 */
enum class AdvertisementProductPosition(
    override val code: Int,
    override val desc: String,
    val category: AdvertisementProductCategory,
) : EnumField {
    JOB_HOME_TOP_BANNER(1, "채용공고 홈 상단", AdvertisementProductCategory.BANNER),
    JOB_HOME_BOTTOM_BANNER(2, "채용공고 홈 하단", AdvertisementProductCategory.BANNER),
    JOB_DETAIL_BANNER(3, "공고 상세", AdvertisementProductCategory.BANNER),
    BOOTCAMP_LIST_TOP_BANNER(4, "부트캠프 목록 상단", AdvertisementProductCategory.BANNER),
    BOOTCAMP_LIST_BOTTOM_BANNER(5, "부트캠프 목록 하단", AdvertisementProductCategory.BANNER),
    BOOTCAMP_DETAIL_BANNER(6, "부트캠프 상세", AdvertisementProductCategory.BANNER),
}
