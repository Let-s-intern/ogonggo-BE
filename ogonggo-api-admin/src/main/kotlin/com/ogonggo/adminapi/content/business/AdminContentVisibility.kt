package com.ogonggo.adminapi.content.business

import com.ogonggo.core.enumeration.EnumField

/**
 * 관리자 콘솔이 보여 주는 노출 여부다. 게시 상태 네 값을 둘로 접는다.
 * 게시 중만 노출이고 초안·숨김·보관은 비노출이다. 운영자가 구분할 것은 사용자에게 내놓았는가 하나다.
 * 부트캠프는 노출이어도 모집 상태와 공개 기간이 맞아야 사용자 목록에 나온다.
 */
enum class AdminContentVisibility(
    override val code: Int,
    override val desc: String,
) : EnumField {
    VISIBLE(1, "노출"),
    HIDDEN(2, "비노출"),
    ;

    val published: Boolean
        get() = this == VISIBLE

    companion object {
        fun of(published: Boolean): AdminContentVisibility = if (published) VISIBLE else HIDDEN
    }
}
