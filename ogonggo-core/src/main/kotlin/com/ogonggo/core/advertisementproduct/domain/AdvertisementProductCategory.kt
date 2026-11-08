package com.ogonggo.core.advertisementproduct.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 광고 상품을 묶는 단위다. 광고 신청 폼에서 기업 담당자는 상품이 아니라 이 카테고리를 고른다.
 *
 * 인스타그램·카카오톡 채널은 이름이 정해지지 않아 번호로 둔다(2026-11-07, 이름 확인 필요).
 */
enum class AdvertisementProductCategory(
    override val code: Int,
    override val desc: String,
) : EnumField {
    JOB_POSTING(1, "광고 공고"),
    BANNER(2, "배너"),
    INSTAGRAM_1(3, "인스타그램 1"),
    INSTAGRAM_2(4, "인스타그램 2"),
    KAKAO_1(5, "카카오톡 채널 1"),
    KAKAO_2(6, "카카오톡 채널 2"),
    ;

    /** 오공고 화면에 직접 노출하는 카테고리다. 나머지는 운영자가 채널에 직접 올리므로 노출 설정이 없다. */
    val exposable: Boolean
        get() = this == JOB_POSTING || this == BANNER
}
