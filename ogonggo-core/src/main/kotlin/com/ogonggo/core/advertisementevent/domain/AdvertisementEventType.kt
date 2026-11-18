package com.ogonggo.core.advertisementevent.domain

import com.ogonggo.core.enumeration.EnumField

enum class AdvertisementEventType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    IMPRESSION(1, "노출"),
    CLICK(2, "클릭"),
}

enum class AdvertisementEventAudienceType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    MEMBER(1, "회원"),
    NON_MEMBER(2, "비회원"),
    UNSPECIFIED(3, "희망 직무 미설정"),
}
