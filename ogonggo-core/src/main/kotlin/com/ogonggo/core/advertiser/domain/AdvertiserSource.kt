package com.ogonggo.core.advertiser.domain

import com.ogonggo.core.enumeration.EnumField

/** 광고주가 목록에 들어온 길이다. 비즈니스 회원만 회원 광고주이고 나머지는 비회원 광고주다. */
enum class AdvertiserSource(
    override val code: Int,
    override val desc: String,
    val type: AdvertiserType,
) : EnumField {
    COMPANY_MEMBER(1, "비즈니스 회원", AdvertiserType.MEMBER),
    AD_INQUIRY(2, "광고 문의", AdvertiserType.NON_MEMBER),
    MANUAL(3, "직접 추가", AdvertiserType.NON_MEMBER),
}
