package com.ogonggo.core.advertisementevent.domain

import com.ogonggo.core.enumeration.EnumField

enum class AdvertisementEventType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    IMPRESSION(1, "노출"),
    CLICK(2, "클릭"),
}

/** 클릭자 리포트 집계에만 사용하는 내부 분류이며 사용자 선택지로 노출하지 않는다. */
enum class AdvertisementEventAudienceType {
    MEMBER,
    NON_MEMBER,
    UNSPECIFIED,
}
