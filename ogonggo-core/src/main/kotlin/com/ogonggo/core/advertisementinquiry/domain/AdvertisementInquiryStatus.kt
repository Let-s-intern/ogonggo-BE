package com.ogonggo.core.advertisementinquiry.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 영업이 광고 문의를 어디까지 처리했는지다. 새 문의는 접수다.
 *
 * 영업이 잘못 바꾼 상태를 되돌릴 수 있도록 값 사이의 전이를 막지 않는다.
 * 반려를 따로 두어 집행 대상만 걸러 볼 수 있게 한다.
 */
enum class AdvertisementInquiryStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    RECEIVED(1, "접수"),
    CONTACTED(2, "연락함"),
    EXECUTED(3, "집행 완료"),
    REJECTED(4, "반려"),
}
