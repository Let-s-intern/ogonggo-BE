package com.ogonggo.core.advertisementcontract.domain

import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.enumeration.EnumField

/**
 * 광고 계약이 어디까지 왔는지다. 신청 폼으로 들어온 계약은 신청 → 협의 중 → 승인·반려로 가고,
 * 비즈니스 공고 계약은 공고 검수 상태를 그대로 따른다([of]).
 *
 * 영업이 잘못 바꾼 상태를 되돌릴 수 있도록 값 사이의 전이를 막지 않는다.
 */
enum class AdvertisementContractStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    REQUESTED(1, "신청"),
    NEGOTIATING(2, "협의 중"),
    APPROVED(3, "승인"),
    REJECTED(4, "반려"),
    ;

    companion object {
        /** 비즈니스 공고의 검수 상태에 맞는 계약 상태다. 검수 대기는 신청이다. */
        fun of(reviewStatus: ContentReviewStatus): AdvertisementContractStatus = when (reviewStatus) {
            ContentReviewStatus.PENDING -> REQUESTED
            ContentReviewStatus.APPROVED -> APPROVED
            ContentReviewStatus.REJECTED -> REJECTED
        }
    }
}
