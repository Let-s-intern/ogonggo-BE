package com.ogonggo.core.job.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 지원자가 공고에 지원하는 경로다.
 * 부트캠프에도 같은 이름의 값이 있지만 두 도메인이 각각 정하므로 따로 둔다.
 */
enum class JobApplicationMethod(
    override val code: Int,
    override val desc: String,
) : EnumField {
    EXTERNAL_PAGE(1, "외부 페이지"),
    EMAIL(2, "이메일"),
}
