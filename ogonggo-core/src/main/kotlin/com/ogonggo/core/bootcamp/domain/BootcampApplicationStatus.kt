package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 북마크한 부트캠프를 마이페이지 지원·신청 관리에서 어느 단계에 두었는지 나타낸다.
 * 북마크를 등록하거나 해제 후 다시 등록하면 스크랩 단계에서 시작한다.
 * 단계 사이에 선후 관계가 없어 어느 단계에서든 다른 어느 단계로든 옮길 수 있다.
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
}
