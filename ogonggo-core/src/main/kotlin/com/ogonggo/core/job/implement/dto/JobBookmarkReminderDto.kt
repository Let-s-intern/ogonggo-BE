package com.ogonggo.core.job.implement.dto

import java.time.LocalDateTime

/** 페이지 평가에 필요한 일정 스냅샷. entity 대신 core 구현 경계에서 이 값을 전달한다. */
data class JobBookmarkReminderScheduleDto(
    val id: Long,
    val jobId: Long,
    val recruitmentEndAt: LocalDateTime,
    val reminderAt: LocalDateTime,
    val lastBookmarkId: Long?,
)

/** 알림 payload와 수신 주소를 만들 수 있도록 조회한 한 명의 적격 스크랩 대상이다. */
data class JobBookmarkReminderCandidateDto(
    val bookmarkId: Long,
    val jobId: Long,
    val userId: Long,
    val recruitmentEndAt: LocalDateTime,
    val recipientNo: String,
    val recipientName: String,
    val postingTitle: String,
)
