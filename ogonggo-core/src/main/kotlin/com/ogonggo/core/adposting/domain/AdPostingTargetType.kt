package com.ogonggo.core.adposting.domain

import com.ogonggo.core.enumeration.EnumField

/** 광고 공고가 싣는 대상의 종류다. 자리(`AdPostingPlacement`)가 정한다. */
enum class AdPostingTargetType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    JOB(1, "채용공고"),
    BOOTCAMP(2, "부트캠프"),
}
