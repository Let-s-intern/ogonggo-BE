package com.ogonggo.core.bookmark.domain

import com.ogonggo.core.enumeration.EnumField

/** 마이페이지 지원·신청 관리에서 북마크 목록을 고르는 정렬 기준이다. 임의의 정렬 필드는 받지 않는다. */
enum class BookmarkSortType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    /** 북마크를 등록하거나 다시 등록하거나 지원 단계를 옮긴 시각이 최근인 순서다. */
    RECENTLY_SAVED(1, "최근 저장순"),
}
