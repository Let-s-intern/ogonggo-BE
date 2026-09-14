package com.ogonggo.adminapi.job.presentation.request

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.adminapi.job.business.AdminJobUpdateCommand
import com.ogonggo.core.job.domain.JobContentField
import com.ogonggo.core.review.domain.ReviewStatus
import jakarta.validation.constraints.Size

/**
 * 모든 값이 선택이며 넘어온 값만 바꾼다. 등록 경로(`source`)는 바꿀 수 없어 받지 않는다.
 *
 * `fields`는 허용한 본문 칸만 반영하고 목록 밖의 키는 버린다.
 * 빈 문자열은 그 칸을 비우라는 뜻이다.
 */
data class UpdateAdminJobRequest(
    val visibility: AdminContentVisibility?,
    val reviewStatus: ReviewStatus?,
    @field:Size(max = 255) val title: String?,
    val fields: Map<String, String?>?,
) {
    fun toCommand(): AdminJobUpdateCommand {
        if (title != null && title.isBlank()) {
            throw InvalidRequestFieldException("title", "제목을 입력해 주세요.")
        }
        if (reviewStatus == ReviewStatus.REJECTED) {
            throw InvalidRequestFieldException("reviewStatus", "반려는 검수 화면에서 사유와 함께 처리해 주세요.")
        }
        return AdminJobUpdateCommand(
            visibility = visibility,
            reviewStatus = reviewStatus,
            title = title,
            contents = fields.orEmpty()
                .mapNotNull { (key, value) ->
                    JobContentField.fromFieldName(key)?.let { field -> field to value?.takeIf { it.isNotBlank() } }
                }
                .toMap(),
        )
    }
}
