package com.ogonggo.core.advertisementinquiry.domain

import java.time.LocalDate

/** 관리자 콘솔 광고 문의 목록의 선택 조건이다. null이거나 비어 있는 조건은 적용하지 않는다. */
data class AdvertisementInquirySearchCondition(
    val inquiryType: AdvertisementInquiryType? = null,
    val campaignCode: String? = null,
    /** 이 상태 중 하나인 문의를 찾는다. */
    val statuses: Set<AdvertisementInquiryStatus> = emptySet(),
    val memberType: AdvertisementInquiryMemberType? = null,
    val inflowChannel: AdvertisementInquiryInflowChannel? = null,
    val companyNameSource: AdvertisementInquiryCompanyNameSource? = null,
    /** 이 날짜 0시부터 들어온 문의를 찾는다. */
    val createdFrom: LocalDate? = null,
    /** 이 날짜가 끝날 때까지 들어온 문의를 찾는다. 당일을 포함한다. */
    val createdTo: LocalDate? = null,
)
