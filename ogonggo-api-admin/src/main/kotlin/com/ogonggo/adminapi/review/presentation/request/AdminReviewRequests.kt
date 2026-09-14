package com.ogonggo.adminapi.review.presentation.request

import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.core.review.domain.ContentRejection
import com.ogonggo.core.review.domain.ReviewStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 검수 판정이다. `decision`은 APPROVED나 REJECTED만 받는다.
 * 반려에 사유가 없으면 올린 사람이 무엇을 고칠지 알 수 없어 같은 글이 다시 올라온다.
 * 화면에서도 막지만 규칙이 화면에만 있지 않도록 서버도 막는다.
 */
data class DecideReviewRequest(
    val decision: ReviewStatus,
    @field:Size(max = ContentRejection.MAX_REASON_LENGTH) val reason: String?,
) {
    fun requiredReason(): String =
        reason?.takeIf { it.isNotBlank() } ?: throw InvalidRequestFieldException("reason", "반려 사유를 입력해 주세요.")

    fun invalidDecision(): Nothing =
        throw InvalidRequestFieldException("decision", "APPROVED 또는 REJECTED만 보낼 수 있습니다.")
}

/** 사유를 지우는 길은 없다. 반려를 풀려면 검수 대기로 되돌린다. */
data class UpdateRejectionReasonRequest(
    @field:NotBlank(message = "반려 사유를 입력해 주세요.")
    @field:Size(max = ContentRejection.MAX_REASON_LENGTH)
    val reason: String,
)
