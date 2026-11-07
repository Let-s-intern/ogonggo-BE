package com.ogonggo.core.advertisementcontract.domain

import com.ogonggo.core.enumeration.EnumField

/** 광고 신청 폼의 계약 및 계산서 발행 방식이다. */
enum class AdvertisementContractInvoiceMethod(
    override val code: Int,
    override val desc: String,
) : EnumField {
    TAX_INVOICE(1, "세금계산서 발행"),
    NEGOTIATE(2, "담당자와 협의"),
}
