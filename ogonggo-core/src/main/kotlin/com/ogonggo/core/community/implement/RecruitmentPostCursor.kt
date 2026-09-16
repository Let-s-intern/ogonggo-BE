package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPostSortType
import java.time.LocalDate

/**
 * 공개 모집글 목록의 마지막 위치를 표현한다.
 *
 * 정렬 기준이 바뀌거나 필터가 바뀐 커서를 재사용하지 않도록 조회 조건의 키도 함께 보관한다.
 */
data class RecruitmentPostCursor(
    val queryKey: String,
    val sortType: RecruitmentPostSortType,
    val id: Long,
    val deadline: LocalDate? = null,
    val metricCount: Long? = null,
) {
    init {
        require(queryKey.isNotBlank()) { "모집글 커서 조회 조건은 비어 있을 수 없습니다." }
        require(id > 0) { "모집글 커서의 식별자는 양수여야 합니다." }
        when (sortType) {
            RecruitmentPostSortType.LATEST -> require(deadline == null && metricCount == null)
            RecruitmentPostSortType.DEADLINE -> require(deadline != null && metricCount == null)
            RecruitmentPostSortType.VIEW_COUNT,
            RecruitmentPostSortType.COMMENT_COUNT,
            -> require(deadline == null && metricCount != null && metricCount >= 0)
        }
    }
}
