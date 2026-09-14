package com.ogonggo.core.review.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 기업회원이 올린 콘텐츠의 검수 상태다.
 * 크롤러가 수집한 콘텐츠는 사람이 한 건씩 통과시킬 대상이 아니므로 값이 없다.
 */
enum class ReviewStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    PENDING(1, "검수 대기"),
    APPROVED(2, "승인"),
    REJECTED(3, "반려"),
}
