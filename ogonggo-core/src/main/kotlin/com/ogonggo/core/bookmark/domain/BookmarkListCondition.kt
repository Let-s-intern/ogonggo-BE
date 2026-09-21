package com.ogonggo.core.bookmark.domain

/**
 * 채용공고·부트캠프 북마크 목록에만 거는 조건이다.
 * 대상 목록과 같은 필터는 각 도메인의 검색 조건에 두고, 북마크 행이 가진 값으로 고르는 조건만 여기에 둔다.
 */
data class BookmarkListCondition(
    /** 지원·신청 관리 단계. 비어 있으면 모든 단계를 고른다. */
    val applicationStatus: ApplicationStatus? = null,
    val sortType: BookmarkSortType = BookmarkSortType.RECENTLY_SAVED,
) {
    companion object {
        val NONE = BookmarkListCondition()
    }
}
