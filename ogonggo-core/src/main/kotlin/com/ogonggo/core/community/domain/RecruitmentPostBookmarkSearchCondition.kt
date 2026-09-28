package com.ogonggo.core.community.domain

import com.ogonggo.core.bookmark.domain.BookmarkSortType

/**
 * 사이드·스터디 모집글 북마크 목록에서 고르는 선택 필터다. 값이 null이면 그 조건을 걸지 않는다.
 * 지원 이력 목록과 같은 필터를 두어 마이페이지 지원·신청 관리의 스크랩 칸과 나머지 칸을 같은 조건으로 좁힌다.
 */
data class RecruitmentPostBookmarkSearchCondition(
    val recruitmentStatus: RecruitmentStatus? = null,
    val recruitmentType: RecruitmentType? = null,
    /** 모집글 제목에 포함되는지로 찾는다. 비어 있으면 검색하지 않는다. */
    val keyword: String? = null,
    val sortType: BookmarkSortType = BookmarkSortType.RECENTLY_SAVED,
) {
    companion object {
        val NONE = RecruitmentPostBookmarkSearchCondition()
    }
}
