package com.ogonggo.core.advertisementinquiry.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 광고 문의의 종류다. 유형마다 받는 값과 후속 대응이 다르다.
 *
 * FREE_PROMOTION과 LAUNCH_ALERT는 무료 홍보 폼의 두 선택지다. FREE_PROMOTION은 바로 연락할 대상이고
 * LAUNCH_ALERT는 출시 안내만 받을 대기 명단이다. 두 유형은 희망 홍보 채널과 설문 답변을 받는다.
 * PAID_ADVERTISEMENT는 광고 신청 페이지에서 상품을 고르기 어려운 기업이 남기는 문의다.
 * 광고 목적과 희망 예산을 받고, 영업이 이메일로 답한다.
 */
enum class AdvertisementInquiryType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    FREE_PROMOTION(1, "지금 바로 무료 홍보 할래요"),
    LAUNCH_ALERT(2, "지금은 출시 알림만 받을래요"),
    PAID_ADVERTISEMENT(3, "유료 광고 문의"),
    ;

    /** 무료 홍보 폼에서 온 문의인지다. 희망 홍보 채널과 설문 답변을 받는다. */
    val isFreePromotionForm: Boolean
        get() = this != PAID_ADVERTISEMENT
}
