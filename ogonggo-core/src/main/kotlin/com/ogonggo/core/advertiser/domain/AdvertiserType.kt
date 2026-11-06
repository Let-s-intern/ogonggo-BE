package com.ogonggo.core.advertiser.domain

import com.ogonggo.core.enumeration.EnumField

/** 광고주가 비즈니스 회원인지다. 들어온 길(`AdvertiserSource`)이 정하며 바뀌지 않는다. */
enum class AdvertiserType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    MEMBER(1, "회원"),
    NON_MEMBER(2, "비회원"),
}
