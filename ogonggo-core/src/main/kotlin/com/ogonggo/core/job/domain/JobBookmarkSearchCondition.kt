package com.ogonggo.core.job.domain

import com.ogonggo.core.bookmark.domain.BookmarkSortType

/**
 * 채용공고 북마크 목록에만 거는 조건이다. 공개 목록과 같은 필터는 [JobSearchCondition]에 둔다.
 * 값이 null이면 그 조건을 걸지 않는다.
 */
data class JobBookmarkSearchCondition(
    /** 지원·신청 관리 단계. */
    val applicationStatus: JobApplicationStatus? = null,
    /** 모집 상태. 저장하지 않고 조회 시각 기준으로 계산한다. */
    val recruitmentStatus: JobRecruitmentStatus? = null,
    val sortType: BookmarkSortType = BookmarkSortType.RECENTLY_SAVED,
) {
    companion object {
        val NONE = JobBookmarkSearchCondition()
    }
}
