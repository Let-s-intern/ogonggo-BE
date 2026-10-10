package com.ogonggo.userapi.notification.intake.business.dto

/** 한 번의 리마인드 적재 페이지 결과와 실행 중에만 쓰는 다음 커서다. */
internal data class ReminderEnqueueResultDto(
    val workCount: Int = 0,
    val candidateCount: Int = 0,
    val queuedCount: Int = 0,
    val skippedCount: Int = 0,
    val lastBookmarkId: Long? = null,
)
