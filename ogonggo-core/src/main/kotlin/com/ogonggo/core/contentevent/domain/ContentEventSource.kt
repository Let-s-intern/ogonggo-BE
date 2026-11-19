package com.ogonggo.core.contentevent.domain

import com.ogonggo.core.enumeration.EnumField

/** 콘텐츠 이벤트가 어디서 들어왔는지(유입 경로)다. 광고 카드·배너로 들어왔으면 노출 식별자도 함께 싣는다. */
enum class ContentEventSource(
    override val code: Int,
    override val desc: String,
) : EnumField {
    AD_CARD(1, "광고 카드"),
    BANNER(2, "배너"),
    LIST(3, "목록"),
    SEARCH(4, "검색"),
    DIRECT(5, "직접 방문"),
    EXTERNAL(6, "외부 유입"),
    OTHER(7, "기타"),
}
