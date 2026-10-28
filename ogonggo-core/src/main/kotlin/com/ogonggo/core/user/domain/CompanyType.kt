package com.ogonggo.core.user.domain

import com.ogonggo.core.enumeration.EnumField

/** 기업 회원이 가입할 때 고른 기업 형태다. 회원이 고른 값이며 확인하지 않는다. */
enum class CompanyType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    LARGE(1, "대기업"),
    MID_SIZED(2, "중견기업"),
    SMALL(3, "중소기업"),
    STARTUP(4, "스타트업"),
    FOREIGN(5, "외국계"),
    PUBLIC(6, "공공기관·공기업"),
    NON_PROFIT(7, "비영리·단체"),
}
