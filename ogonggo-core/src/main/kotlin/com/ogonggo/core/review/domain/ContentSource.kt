package com.ogonggo.core.review.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 콘텐츠의 등록 경로다. 따로 저장하지 않고 소유자 유무로 정한다.
 * 등록 경로는 바꿀 수 없으므로 칸을 두면 소유자와 어긋나는 상태만 생긴다.
 */
enum class ContentSource(
    override val code: Int,
    override val desc: String,
) : EnumField {
    CRAWLER(1, "크롤링"),
    COMPANY(2, "비즈니스 등록"),
    ;

    companion object {
        fun of(ownerUserId: Long?): ContentSource = if (ownerUserId == null) CRAWLER else COMPANY
    }
}
