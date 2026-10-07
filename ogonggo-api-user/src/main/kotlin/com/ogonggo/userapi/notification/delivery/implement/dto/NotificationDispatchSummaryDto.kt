package com.ogonggo.userapi.notification.delivery.implement.dto

/** 단일 발송 묶음 또는 한 번의 실행에서 처리한 결과 수와 중단 사유다. */
internal data class NotificationDispatchSummaryDto(
    val sentCount: Int = 0,
    val failedCount: Int = 0,
    val unknownCount: Int = 0,
    val unresolvedCount: Int = 0,
    val executorRejected: Boolean = false,
) {
    val hasResults: Boolean
        get() = sentCount + failedCount + unknownCount + unresolvedCount > 0

    val shouldStopNextBatch: Boolean
        get() = executorRejected || unresolvedCount > 0

    fun include(outcome: NotificationDispatchOutcome): NotificationDispatchSummaryDto = when (outcome) {
        NotificationDispatchOutcome.SENT -> copy(sentCount = sentCount + 1)
        NotificationDispatchOutcome.FAILED -> copy(failedCount = failedCount + 1)
        NotificationDispatchOutcome.UNKNOWN -> copy(unknownCount = unknownCount + 1)
        NotificationDispatchOutcome.UNRESOLVED -> copy(unresolvedCount = unresolvedCount + 1)
    }

    operator fun plus(other: NotificationDispatchSummaryDto) = NotificationDispatchSummaryDto(
        sentCount = sentCount + other.sentCount,
        failedCount = failedCount + other.failedCount,
        unknownCount = unknownCount + other.unknownCount,
        unresolvedCount = unresolvedCount + other.unresolvedCount,
        executorRejected = executorRejected || other.executorRejected,
    )
}

internal enum class NotificationDispatchOutcome {
    SENT,
    FAILED,
    UNKNOWN,
    UNRESOLVED,
}
