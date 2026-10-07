package com.ogonggo.core.job.implement.event

import java.time.LocalDateTime

/** 공고의 모집 마감 시각이 바뀌었음을 같은 DB 트랜잭션 안에서 전달한다. */
data class JobRecruitmentDeadlineChangedEvent(
    val jobId: Long,
    val previousEndAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
)
