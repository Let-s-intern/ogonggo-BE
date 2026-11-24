package com.ogonggo.core.contentevent.domain

import com.ogonggo.core.enumeration.EnumField

/** 콘텐츠 이벤트가 가리키는 콘텐츠의 종류다. */
enum class ContentEventTargetType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    JOB(1, "채용공고"),
    BOOTCAMP(2, "부트캠프"),
    CONCERN(3, "취준고민"),
    RECRUITMENT_POST(4, "사이드·스터디 모집글"),
}
