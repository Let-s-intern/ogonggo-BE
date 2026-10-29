package com.ogonggo.core.user.domain

import com.ogonggo.core.enumeration.EnumField

/** 기업 회원이 가입할 때 고른 직원 수 구간이다. */
enum class CompanyEmployeeCountRange(
    override val code: Int,
    override val desc: String,
) : EnumField {
    RANGE_1_9(1, "1~9명"),
    RANGE_10_49(2, "10~49명"),
    RANGE_50_99(3, "50~99명"),
    RANGE_100_299(4, "100~299명"),
    RANGE_300_999(5, "300~999명"),
    RANGE_1000_PLUS(6, "1,000명 이상"),
}
