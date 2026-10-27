package com.ogonggo.core.advertisementinquiry.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 문의를 남긴 사람이 오공고 회원이었는지다. 저장할 때 서버가 토큰으로 정하고 클라이언트가 보낸 값은 받지 않는다.
 * 문의 시점의 구분이라 나중에 회원의 역할이 바뀌어도 다시 계산하지 않는다.
 */
enum class AdvertisementInquiryMemberType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    COMPANY(1, "기업 회원"),
    USER(2, "일반 회원"),
    GUEST(3, "비회원"),
}
