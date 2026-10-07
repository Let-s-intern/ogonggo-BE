package com.ogonggo.userapi.notification.intake.business.dto

/** 한 번의 리마인드 적재 작업에서 처리한 일정·후보·결과 건수다. */
internal data class ReminderEnqueueResultDto(
    val workCount: Int = 0,
    val candidateCount: Int = 0,
    val queuedCount: Int = 0,
    val skippedCount: Int = 0,
) {
    operator fun plus(other: ReminderEnqueueResultDto) = ReminderEnqueueResultDto(
        workCount = workCount + other.workCount,
        candidateCount = candidateCount + other.candidateCount,
        queuedCount = queuedCount + other.queuedCount,
        skippedCount = skippedCount + other.skippedCount,
    )
}
