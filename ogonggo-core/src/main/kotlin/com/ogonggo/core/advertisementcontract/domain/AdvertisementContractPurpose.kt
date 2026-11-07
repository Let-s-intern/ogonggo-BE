package com.ogonggo.core.advertisementcontract.domain

import com.ogonggo.core.enumeration.EnumField

/** 광고 신청 폼에서 고르는 광고 목적이다. */
enum class AdvertisementContractPurpose(
    override val code: Int,
    override val desc: String,
) : EnumField {
    FAST_APPLICANTS(1, "지원자 빠르게 확보"),
    FIT_APPLICANTS(2, "적합한 지원자 확보"),
    BRAND_AWARENESS(3, "브랜드 인지도 확대"),
}
