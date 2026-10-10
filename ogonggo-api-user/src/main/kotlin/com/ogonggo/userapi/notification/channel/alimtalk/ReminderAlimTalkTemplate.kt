package com.ogonggo.userapi.notification.channel.alimtalk

/** 알림톡 요청에 사용할 provider 등록 템플릿 코드다. 예약 시각은 업무 일정이 결정한다. */
enum class ReminderAlimTalkTemplate(
    val templateCode: String,
) {
    JOB_BOOKMARK_REMINDER("clip_remind"),
}
