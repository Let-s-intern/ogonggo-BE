package com.ogonggo.core.job.implement.event

import java.time.LocalDateTime

/** 마감일 변경과 같은 DB 트랜잭션 안에서 리마인드 일정을 갱신한다. */
data class JobRecruitmentDeadlineChangedEvent(
    val jobId: Long,
    val previousEndAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val changedAt: LocalDateTime,
)
