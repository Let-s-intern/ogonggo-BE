package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.bookmark.domain.BookmarkSortType

/**
 * 부트캠프 북마크 목록에만 거는 조건이다. 모집 상태처럼 공개 목록과 같은 필터는 [BootcampSearchCondition]에 둔다.
 * 값이 null이면 그 조건을 걸지 않는다.
 */
data class BootcampBookmarkSearchCondition(
    /** 지원·신청 관리 단계. */
    val applicationStatus: BootcampApplicationStatus? = null,
    val sortType: BookmarkSortType = BookmarkSortType.RECENTLY_SAVED,
) {
    companion object {
        val NONE = BootcampBookmarkSearchCondition()
    }
}
