package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 북마크한 부트캠프를 마이페이지 지원·신청 관리에서 어느 단계에 두었는지 나타낸다.
 * 북마크를 등록하거나 해제 후 다시 등록하면 스크랩 단계에서 시작한다.
 */
enum class BootcampApplicationStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    SCRAPPED(1, "스크랩"),
    PREPARING(2, "신청 전"),
    APPLIED(3, "신청 완료"),
    IN_PROGRESS(4, "활동 중"),
    COMPLETED(5, "활동 완료"),
    ;

    /**
     * 이 단계로 옮길 수 있는 출발 단계다. 지금은 스크랩과 신청 전 사이만 오갈 수 있다.
     * 신청 완료·활동 중·활동 완료로 옮기거나 그 단계에서 되돌리는 흐름은 아직 정하지 않았다.
     */
    fun movableFrom(): Set<BootcampApplicationStatus> = when (this) {
        SCRAPPED -> setOf(PREPARING)
        PREPARING -> setOf(SCRAPPED)
        APPLIED, IN_PROGRESS, COMPLETED -> emptySet()
    }
}
