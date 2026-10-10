package com.ogonggo.core.concern.domain

/**
 * 관리자 콘솔이 미삭제 고민글을 고를 때 쓰는 선택 필터다.
 * 값이 null이면 그 조건을 걸지 않고, 여러 값은 모두 AND로 묶는다.
 */
data class ConcernConsoleSearchCondition(
    /** true면 노출 중인 고민글만, false면 숨긴 고민글만 고른다. */
    val visible: Boolean? = null,
    val category: ConcernCategory? = null,
    /** 제목에 포함되는지로 찾는다. 비어 있으면 검색하지 않는다. */
    val keyword: String? = null,
)
