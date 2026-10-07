package com.ogonggo.core.job.implement.dto

import java.time.LocalDateTime

/** 알림 payload와 수신 주소를 만들 수 있도록 조회한 한 명의 적격 스크랩 대상이다. */
data class JobBookmarkReminderCandidateDto(
    val bookmarkId: Long,
    val jobId: Long,
    val userId: Long,
    val recruitmentEndAt: LocalDateTime,
    val reminderAt: LocalDateTime,
    val recipientNo: String,
    val recipientName: String,
    val postingTitle: String,
)
