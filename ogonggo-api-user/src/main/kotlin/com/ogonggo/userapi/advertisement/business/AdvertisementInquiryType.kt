package com.ogonggo.userapi.advertisement.business

import com.ogonggo.core.enumeration.EnumField

/**
 * 광고 문의 폼에서 기업 담당자가 고르는 두 선택지다.
 *
 * 두 값의 후속 대응이 다르다. FREE_PROMOTION은 바로 연락할 대상이고
 * LAUNCH_ALERT는 출시 안내만 받을 대기 명단이다.
 */
enum class AdvertisementInquiryType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    FREE_PROMOTION(1, "지금 바로 무료 홍보 할래요"),
    LAUNCH_ALERT(2, "지금은 출시 알림만 받을래요"),
}
