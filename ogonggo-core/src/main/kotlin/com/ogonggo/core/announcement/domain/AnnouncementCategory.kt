package com.ogonggo.core.announcement.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 공지의 카테고리다. 공지사항 화면의 `전체 카테고리` 가 이 순서대로 칸을 나눈다(LC-3614, 2026-11-12 결정).
 *
 * `NEW_JOB_NOTE` 는 나중에 백엔드가 주기적으로 새로 올라온 공고 수를 정리해 자동으로 올릴 카테고리다. 자동 등록을 붙이기
 * 전까지는 운영자가 관리자 콘솔에서 직접 올린다.
 *
 * `BUSINESS_NOTICE` 는 기업회원에게 알리는 공지다(2026-11-13 추가).
 */
enum class AnnouncementCategory(
    override val code: Int,
    override val desc: String,
) : EnumField {
    OGONGGO_UPDATE(1, "오공고 업데이트"),
    NEW_JOB_NOTE(2, "새로운 공고 노트"),
    SERVICE_NOTICE(3, "서비스 안내"),
    BUSINESS_NOTICE(4, "비즈니스 공지"),
}
