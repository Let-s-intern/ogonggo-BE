package com.ogonggo.core.notice.domain

/** 관리자 콘솔 공지 목록의 선택 조건이다. null인 조건은 적용하지 않는다. */
data class NoticeManagementSearchCondition(
    val published: Boolean? = null,
    val pinned: Boolean? = null,
    /** 제목에서 대소문자를 가리지 않고 부분 일치로 찾는다. */
    val keyword: String? = null,
)
