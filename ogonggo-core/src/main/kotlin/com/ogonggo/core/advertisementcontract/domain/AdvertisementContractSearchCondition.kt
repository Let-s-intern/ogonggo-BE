package com.ogonggo.core.advertisementcontract.domain

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import java.time.LocalDate

/** 광고 계약 목록의 선택 조건이다. 관리자 콘솔 목록과 회원의 광고 신청 내역이 쓴다. null이거나 비어 있는 조건은 적용하지 않는다. */
data class AdvertisementContractSearchCondition(
    val source: AdvertisementContractSource? = null,
    /** 이 회원이 신청한 계약을 찾는다. */
    val userId: Long? = null,
    /** 이 상태 중 하나인 계약을 찾는다. */
    val statuses: Set<AdvertisementContractStatus> = emptySet(),
    val promotionTarget: AdvertisementContractPromotionTarget? = null,
    /** 신청 때 이 카테고리를 고른 계약을 찾는다. */
    val category: AdvertisementProductCategory? = null,
    /** 기업명에 이 글자가 들어간 계약을 찾는다. */
    val companyName: String? = null,
    /** 이 날짜 0시부터 생긴 계약을 찾는다. */
    val createdFrom: LocalDate? = null,
    /** 이 날짜가 끝날 때까지 생긴 계약을 찾는다. 당일을 포함한다. */
    val createdTo: LocalDate? = null,
)
