package com.ogonggo.core.announcement.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 공지를 노출할 때 푸시 알림을 보낼 대상이다. `NONE` 이면 노출해도 보내지 않는다(2026-11-13 결정).
 *
 * 푸시는 공지 하나에 한 번만 간다. 노출 상태가 되는 순간(노출로 등록, 숨김에서 노출로 변경, 노출 중인 공지에 대상을 지정)
 * 보내고, 그 뒤에는 숨겼다가 다시 노출해도 보내지 않는다.
 */
enum class AnnouncementPushTarget(
    override val code: Int,
    override val desc: String,
) : EnumField {
    NONE(1, "보내지 않음"),
    ALL(2, "전체 회원"),
    GENERAL_MEMBERS(3, "일반 회원"),
    COMPANY_MEMBERS(4, "기업 회원"),
}
