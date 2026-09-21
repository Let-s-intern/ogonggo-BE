package com.ogonggo.core.bookmark.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 북마크한 채용공고·부트캠프를 마이페이지 지원·신청 관리에서 어느 단계에 두었는지 나타낸다.
 * 북마크를 등록하거나 해제 후 다시 등록하면 스크랩 단계에서 시작한다.
 */
enum class ApplicationStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    SCRAPPED(1, "스크랩"),
    PREPARING(2, "지원 준비 중"),
    APPLIED(3, "지원 완료"),
    INTERVIEWING(4, "면접"),
    PASSED(5, "합격"),
    FAILED(6, "불합격"),
    ;

    /**
     * 이 단계로 옮길 수 있는 출발 단계다. 지금은 스크랩과 지원 준비 중 사이만 오갈 수 있다.
     * 지원 완료·면접·합격·불합격으로 옮기거나 그 단계에서 되돌리는 흐름은 아직 정하지 않았다.
     */
    fun movableFrom(): Set<ApplicationStatus> = when (this) {
        SCRAPPED -> setOf(PREPARING)
        PREPARING -> setOf(SCRAPPED)
        APPLIED, INTERVIEWING, PASSED, FAILED -> emptySet()
    }
}
