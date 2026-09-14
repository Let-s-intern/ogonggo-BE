package com.ogonggo.core.review.domain

import com.ogonggo.core.enumeration.EnumField

/** 검수와 반려 기록이 가리키는 콘텐츠 종류다. */
enum class ReviewContentType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    JOB(1, "채용공고"),
    BOOTCAMP(2, "부트캠프"),
}
