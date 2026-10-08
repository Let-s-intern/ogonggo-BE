package com.ogonggo.core.concern.domain

import com.ogonggo.core.enumeration.EnumField

enum class ConcernCategory(
    override val code: Int,
    override val desc: String,
) : EnumField {
    JOB_POSTING(1, "공고 질문"),
    JOB_CAREER(2, "직무·커리어"),
    APPLICATION_INTERVIEW(3, "서류·면접"),
    SIDE_EXPERIENCE(4, "사이드·경험"),
    ETC(5, "기타"),
}
