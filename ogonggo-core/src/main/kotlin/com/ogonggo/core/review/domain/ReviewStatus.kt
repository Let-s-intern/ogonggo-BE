package com.ogonggo.core.review.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 기업회원이 올린 콘텐츠와 크롤러가 AI로 값을 채워 보낸 채용공고의 검수 상태다.
 * 검수를 거치지 않고 만든 수집 콘텐츠는 값이 없다.
 */
enum class ReviewStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    PENDING(1, "검수 대기"),
    APPROVED(2, "승인"),
    REJECTED(3, "반려"),
}
