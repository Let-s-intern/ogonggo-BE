package com.ogonggo.core.recruitmentpost.domain

/**
 * 관리자 콘솔이 게시된 적 있는 미삭제 모집글을 고를 때 쓰는 선택 필터다.
 * 임시저장은 작성자만 보는 글이라 늘 뺀다. 값이 null이면 그 조건을 걸지 않고, 여러 값은 모두 AND로 묶는다.
 */
data class RecruitmentPostConsoleSearchCondition(
    /** true면 게시 중인 모집글만, false면 숨긴 모집글만 고른다. */
    val published: Boolean? = null,
    val recruitmentType: RecruitmentPostType? = null,
    val recruitmentStatus: RecruitmentPostRecruitmentStatus? = null,
    /** 제목에 포함되는지로 찾는다. 비어 있으면 검색하지 않는다. */
    val keyword: String? = null,
)
