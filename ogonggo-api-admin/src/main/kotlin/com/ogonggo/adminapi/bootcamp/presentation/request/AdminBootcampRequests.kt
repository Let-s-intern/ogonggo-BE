package com.ogonggo.adminapi.bootcamp.presentation.request

import com.ogonggo.adminapi.bootcamp.business.AdminBootcampUpdateCommand
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.core.bootcamp.domain.BootcampContentField
import com.ogonggo.core.review.domain.ReviewStatus
import jakarta.validation.constraints.Size

/**
 * 채용공고 수정과 같은 계약이다. 모든 값이 선택이며 넘어온 값만 바꾼다.
 * `fields`는 허용한 본문 칸만 반영하고 목록 밖의 키는 버리며, 빈 문자열은 그 칸을 비운다.
 * 상세 내용은 부트캠프의 필수값이라 비울 수 없다.
 */
data class UpdateAdminBootcampRequest(
    val visibility: AdminContentVisibility?,
    val reviewStatus: ReviewStatus?,
    @field:Size(max = 255) val title: String?,
    val fields: Map<String, String?>?,
) {
    fun toCommand(): AdminBootcampUpdateCommand {
        if (title != null && title.isBlank()) {
            throw InvalidRequestFieldException("title", "과정명을 입력해 주세요.")
        }
        if (reviewStatus == ReviewStatus.REJECTED) {
            throw InvalidRequestFieldException("reviewStatus", "반려는 검수 화면에서 사유와 함께 처리해 주세요.")
        }
        val contents = fields.orEmpty()
            .mapNotNull { (key, value) ->
                BootcampContentField.fromFieldName(key)?.let { field -> field to value?.takeIf { it.isNotBlank() } }
            }
            .toMap()
        contents.entries.firstOrNull { (field, value) -> field.required && value == null }?.let { (field, _) ->
            throw InvalidRequestFieldException("fields.${field.fieldName}", "${field.desc}은 비울 수 없습니다.")
        }
        return AdminBootcampUpdateCommand(
            visibility = visibility,
            reviewStatus = reviewStatus,
            title = title,
            contents = contents,
        )
    }
}
