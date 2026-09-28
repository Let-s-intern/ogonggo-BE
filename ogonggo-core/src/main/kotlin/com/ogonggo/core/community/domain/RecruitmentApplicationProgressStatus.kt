package com.ogonggo.core.community.domain

import com.ogonggo.core.enumeration.EnumField

/** 외부 지원 링크 접근 이력에 붙는 사용자의 개인 관리 상태다. */
enum class RecruitmentApplicationProgressStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    PREPARING(1, "지원 준비 중"),
    COMPLETED(2, "지원 완료"),
    IN_PROGRESS(3, "활동 중"),
    ENDED(4, "활동 완료"),
}
