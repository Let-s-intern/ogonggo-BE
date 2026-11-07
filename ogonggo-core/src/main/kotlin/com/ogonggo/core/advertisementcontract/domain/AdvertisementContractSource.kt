package com.ogonggo.core.advertisementcontract.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 광고 계약이 어디서 생겼는지다. 경로마다 채우는 값이 달라 계약이 지켜야 할 규칙도 다르다.
 *
 * [APPLICATION_FORM]은 광고 신청 폼이 받은 값을 모두 가진다.
 * [COMPANY_POSTING]은 기업회원이 비즈니스로 등록한 공고 하나에 하나씩 생기며 그 공고를 가리킨다.
 * 신청 폼에서만 받는 값은 없고, 담당자 연락처는 회원의 기업 정보로 본다.
 */
enum class AdvertisementContractSource(
    override val code: Int,
    override val desc: String,
) : EnumField {
    APPLICATION_FORM(1, "광고 신청"),
    COMPANY_POSTING(2, "비즈니스 공고 등록"),
}
