package com.ogonggo.core.advertisementcontract.domain

import com.ogonggo.core.enumeration.EnumField

/** 광고 신청 폼의 소재 준비 방식이다. */
enum class AdvertisementContractCreativeMethod(
    override val code: Int,
    override val desc: String,
) : EnumField {
    REQUEST_PRODUCTION(1, "공고 정보로 제작 요청"),
    PROVIDE_DIRECTLY(2, "직접 제작한 소재 제공"),
}
