package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus

/**
 * 공개 여부와 무관하게 모든 미삭제 부트캠프를 관리할 때 고르는 선택 필터다.
 * 값이 null이면 그 조건을 걸지 않고, 여러 값은 모두 AND로 묶는다.
 */
data class BootcampManagementSearchCondition(
    /** true면 게시 중인 부트캠프만, false면 게시 중이 아닌 부트캠프만 고른다. */
    val published: Boolean? = null,
    val source: ContentSource? = null,
    val reviewStatus: ReviewStatus? = null,
    val status: BootcampStatus? = null,
    /** 운영 회사명 또는 프로그램명에 포함되는지로 찾는다. 비어 있으면 검색하지 않는다. */
    val keyword: String? = null,
) {
    companion object {
        val NONE = BootcampManagementSearchCondition()
    }
}
