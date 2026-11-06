package com.ogonggo.core.advertiser.domain

/** 광고주 목록 조건이다. 값이 없으면 그 조건을 적용하지 않는다. */
data class AdvertiserSearchCondition(
    val type: AdvertiserType? = null,
    /** 이름·담당자 이름·담당자 이메일에서 대소문자를 가리지 않고 부분 일치로 찾는다. */
    val keyword: String? = null,
)
