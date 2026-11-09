package com.ogonggo.core.advertisementcontract.domain

import com.ogonggo.core.enumeration.EnumField

/** 광고 신청 폼에서 고르는 예정 예산 구간이다. */
enum class AdvertisementContractBudgetRange(
    override val code: Int,
    override val desc: String,
) : EnumField {
    UNDER_500K(1, "50만원 미만"),
    FROM_500K_TO_1M(2, "50만~100만원"),
    FROM_1M_TO_3M(3, "100만~300만원"),
    OVER_3M(4, "300만원 이상"),
    UNDECIDED(5, "아직 정하지 않았어요"),
}
