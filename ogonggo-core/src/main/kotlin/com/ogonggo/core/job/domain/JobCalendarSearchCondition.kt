package com.ogonggo.core.job.domain

/**
 * 채용공고 달력에만 거는 조건이다. 공개 목록과 같은 필터는 [JobSearchCondition]에 둔다.
 * 기본값은 조건을 걸지 않는 것이다.
 */
data class JobCalendarSearchCondition(
    /** 이 사용자가 북마크한 공고만 읽는다. null이면 거르지 않는다. */
    val bookmarkedUserId: Long? = null,
    /** 마감 처리됐거나 모집 종료 일시가 지난 공고를 뺀다. 조회 시각 기준으로 계산한다. */
    val excludeClosed: Boolean = false,
    /** 모집 기간이 겹치는 공고 대신 모집 종료 일시가 조회 범위 안에 있는 공고만 읽는다. */
    val deadlineOnly: Boolean = false,
) {
    companion object {
        val NONE = JobCalendarSearchCondition()
    }
}
