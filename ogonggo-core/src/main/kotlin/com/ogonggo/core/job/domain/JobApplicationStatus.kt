package com.ogonggo.core.job.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 북마크한 채용공고를 마이페이지 지원·신청 관리에서 어느 단계에 두었는지 나타낸다.
 * 북마크를 등록하거나 해제 후 다시 등록하면 스크랩 단계에서 시작한다.
 * 단계 사이에 선후 관계가 없어 어느 단계에서든 다른 어느 단계로든 옮길 수 있다.
 */
enum class JobApplicationStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    SCRAPPED(1, "스크랩"),
    PREPARING(2, "지원 준비 중"),
    APPLIED(3, "지원 완료"),
    INTERVIEWING(4, "면접"),
    PASSED(5, "합격"),
    FAILED(6, "불합격"),
}
