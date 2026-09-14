package com.ogonggo.core.job.domain

import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus

/**
 * 게시 상태와 무관하게 모든 미삭제 채용공고를 관리할 때 고르는 선택 필터다.
 * 값이 null이면 그 조건을 걸지 않고, 여러 값은 모두 AND로 묶는다.
 */
data class JobManagementSearchCondition(
    /** true면 게시 중인 공고만, false면 게시 중이 아닌 공고만 고른다. */
    val published: Boolean? = null,
    val source: ContentSource? = null,
    val reviewStatus: ReviewStatus? = null,
    val recruitmentStatus: JobRecruitmentStatus? = null,
    /** 회사명 또는 공고 제목에 포함되는지로 찾는다. 비어 있으면 검색하지 않는다. */
    val keyword: String? = null,
) {
    companion object {
        val NONE = JobManagementSearchCondition()
    }
}
