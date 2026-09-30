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

/** 배열 순서가 노출 순서다. 빈 배열은 오늘의 공고를 비우라는 뜻이다. */
data class ReplaceAdminTodayJobsRequest(
    val jobIds: List<Long?>,
) {
    /** 배열 요소의 제약은 Bean Validation으로 선언할 수 없어 여기서 확인한다. 요소에 null이 와도 500이 되지 않게 한다. */
    fun toJobIds(): List<Long> {
        val ids = jobIds.map { id ->
            id?.takeIf { it > 0 } ?: throw InvalidRequestFieldException("jobIds", "채용공고 식별자는 양수여야 합니다.")
        }
        if (ids.distinct().size != ids.size) {
            throw InvalidRequestFieldException("jobIds", "같은 공고를 두 번 넣을 수 없습니다.")
        }
        return ids
    }
}
