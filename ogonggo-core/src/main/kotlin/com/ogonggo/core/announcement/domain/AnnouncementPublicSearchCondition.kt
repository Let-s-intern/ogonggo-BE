package com.ogonggo.core.announcement.domain

/** 사용자 공지 목록의 선택 조건이다. null인 조건은 적용하지 않는다. */
data class AnnouncementPublicSearchCondition(
    val category: AnnouncementCategory? = null,
    /** 고정 공지만(true) 또는 고정하지 않은 공지만(false). 공지사항 화면의 `추천` 카드는 고정 공지다. */
    val pinned: Boolean? = null,
    /** 제목·요약·본문에서 대소문자를 가리지 않고 부분 일치로 찾는다. */
    val keyword: String? = null,
)
