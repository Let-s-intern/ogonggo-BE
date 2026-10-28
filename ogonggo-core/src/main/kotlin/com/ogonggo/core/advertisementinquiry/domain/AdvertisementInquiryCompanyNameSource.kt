package com.ogonggo.core.advertisementinquiry.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 문의의 회사명을 어떻게 채웠는지다. 같은 회사가 다른 표기로 들어온 행을 영업이 찾아 정리할 수 있게 표시한다.
 *
 * 클라이언트가 보낸 값을 그대로 믿는다. 검증 수단이 아니라 정리할 행을 고르는 표시이기 때문이다.
 */
enum class AdvertisementInquiryCompanyNameSource(
    override val code: Int,
    override val desc: String,
) : EnumField {
    SEARCH(1, "검색"),
    MANUAL(2, "직접 입력"),
    MEMBER_PROFILE(3, "회원 정보"),
}
