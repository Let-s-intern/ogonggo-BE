package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.enumeration.EnumField

/** 부트캠프 목록에서 클라이언트가 고를 수 있는 정렬 기준이다. 임의의 정렬 필드는 받지 않는다. */
enum class BootcampSortType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    LATEST(1, "최신순"),
    VIEW_COUNT(2, "조회수순"),
}
