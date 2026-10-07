package com.ogonggo.userapi.notification.channel.alimtalk

import com.ogonggo.core.notification.domain.NotificationFailureCategory

/** NHN API 요청 결과를 정규화한다. Error는 명시적 거절과 접수 여부 미확정 오류를 모두 포함한다. */
sealed interface NhnAlimTalkResult {
    data class Accepted(val requestId: String?) : NhnAlimTalkResult
    data class Error(
        val resultCode: String?,
        val failureCategory: NotificationFailureCategory,
        val description: String,
    ) : NhnAlimTalkResult
    data object InvalidResponse : NhnAlimTalkResult
}
