package com.ogonggo.core.contentdailystat.implement.dto

/**
 * 한 회원이 기간 안에 한 활동이다. 희망 직무별 나눔처럼 회원 속성으로 묶을 때 쓴다. 비회원은 없다.
 * [views]는 30분 안 재조회를 뺀 상세 조회수다. [wishJob]은 렛츠커리어 희망 직무 원문이며 없으면 null이다.
 */
data class ContentMemberActivityDto(
    val userId: Long,
    val wishJob: String?,
    val views: Long,
    val viewed: Boolean,
    val ctaClicked: Boolean,
    val adClicked: Boolean,
)
