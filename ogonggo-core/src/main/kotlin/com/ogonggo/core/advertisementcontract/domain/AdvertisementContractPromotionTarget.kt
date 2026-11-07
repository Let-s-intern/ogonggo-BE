package com.ogonggo.core.advertisementcontract.domain

import com.ogonggo.core.enumeration.EnumField

/** 광고 계약으로 홍보하는 것이 채용공고인지 부트캠프인지다. */
enum class AdvertisementContractPromotionTarget(
    override val code: Int,
    override val desc: String,
) : EnumField {
    JOB(1, "채용 공고"),
    BOOTCAMP(2, "교육·부트캠프"),
}
