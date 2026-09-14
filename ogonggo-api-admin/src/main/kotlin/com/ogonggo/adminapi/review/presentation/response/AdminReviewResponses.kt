package com.ogonggo.adminapi.review.presentation.response

import com.ogonggo.adminapi.review.business.AdminReviewDecisionResult
import com.ogonggo.adminapi.review.business.AdminReviewItem
import com.ogonggo.core.review.domain.ReviewContentType
import com.ogonggo.core.review.domain.ReviewStatus
import com.ogonggo.core.review.implement.dto.ContentRejectionDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminReviewItemResponse(
    val type: ReviewContentType,
    val id: Long,
    val title: String,
    val companyName: String,
    val registeredAt: LocalDateTime,
    val sourceUrl: String?,
    @Schema(description = "화면에 그대로 보여 줄 한국어 라벨과 값입니다.")
    val meta: List<AdminReviewMetaResponse>,
    val sections: List<AdminReviewSectionResponse>,
) {
    companion object {
        internal fun from(result: AdminReviewItem): AdminReviewItemResponse = AdminReviewItemResponse(
            type = result.type,
            id = result.id,
            title = result.title,
            companyName = result.companyName,
            registeredAt = result.registeredAt,
            sourceUrl = result.sourceUrl,
            meta = result.meta.map { AdminReviewMetaResponse(it.label, it.value) },
            sections = result.sections.map { AdminReviewSectionResponse(it.field, it.label, it.body) },
        )
    }
}

data class AdminReviewMetaResponse(
    val label: String,
    val value: String,
)

data class AdminReviewSectionResponse(
    @Schema(description = "본문을 고칠 때 수정 API의 fields 키로 쓰는 칸 이름입니다. 고칠 수 없는 섹션은 빈 문자열입니다.")
    val field: String,
    val label: String,
    val body: String,
)

data class AdminReviewDecisionResponse(
    val type: ReviewContentType,
    val id: Long,
    val reviewStatus: ReviewStatus,
    @Schema(description = "판정 뒤에 남은 검수 대기 건수입니다. 채용공고와 부트캠프를 합합니다.")
    val remaining: Long,
) {
    companion object {
        internal fun from(result: AdminReviewDecisionResult): AdminReviewDecisionResponse = AdminReviewDecisionResponse(
            type = result.type,
            id = result.id,
            reviewStatus = result.reviewStatus,
            remaining = result.remaining,
        )
    }
}

data class AdminRejectionResponse(
    val type: ReviewContentType,
    val id: Long,
    val title: String,
    val companyName: String,
    val reason: String,
    val rejectedAt: LocalDateTime,
    val reasonUpdatedAt: LocalDateTime?,
    @Schema(description = "반려한 콘텐츠가 삭제되지 않고 남아 있는지 나타냅니다.")
    val contentExists: Boolean,
) {
    companion object {
        internal fun from(rejection: ContentRejectionDto): AdminRejectionResponse = AdminRejectionResponse(
            type = rejection.contentType,
            id = rejection.contentId,
            title = rejection.title,
            companyName = rejection.companyName,
            reason = rejection.reason,
            rejectedAt = rejection.rejectedAt,
            reasonUpdatedAt = rejection.reasonUpdatedAt,
            contentExists = rejection.contentExists,
        )
    }
}
