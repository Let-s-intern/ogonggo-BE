package com.ogonggo.adminapi.job.presentation.request

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.adminapi.job.business.AdminJobUpdateCommand
import com.ogonggo.adminapi.job.business.AdminJobVisibilityChangeCommand
import com.ogonggo.core.job.domain.JobContentField
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.job.implement.dto.TodayJobDto
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/** 콘솔 목록 한 페이지(최대 100건)를 여러 장 골라도 넉넉하고, 한 트랜잭션의 잠금이 지나치게 길어지지 않을 만큼으로 둔다. */
private const val MAX_VISIBILITY_CHANGE_IDS = 1000

/**
 * 모든 값이 선택이며 넘어온 값만 바꾼다. 등록 경로(`source`)는 바꿀 수 없어 받지 않는다.
 *
 * `fields`는 허용한 본문 칸만 반영하고 목록 밖의 키는 버린다.
 * 빈 문자열은 그 칸을 비우라는 뜻이다.
 */
data class UpdateAdminJobRequest(
    val visibility: AdminContentVisibility?,
    val reviewStatus: ContentReviewStatus?,
    @field:Size(max = 255) val title: String?,
    val fields: Map<String, String?>?,
) {
    fun toCommand(): AdminJobUpdateCommand {
        if (title != null && title.isBlank()) {
            throw InvalidRequestFieldException("title", "제목을 입력해 주세요.")
        }
        if (reviewStatus == ContentReviewStatus.REJECTED) {
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
    @field:Valid val jobs: List<AdminTodayJobRequest?>,
) {
    /** 요소에 null이 와도 500이 되지 않게 여기서 확인한다. 요소 안의 값은 Bean Validation이 먼저 확인한다. */
    fun toDtos(): List<TodayJobDto.Request> {
        val todayJobs = jobs.map { job ->
            job?.toDto() ?: throw InvalidRequestFieldException("jobs", "오늘의 공고 항목을 비울 수 없습니다.")
        }
        if (todayJobs.map(TodayJobDto.Request::jobId).let { it.distinct().size != it.size }) {
            throw InvalidRequestFieldException("jobs", "같은 공고를 두 번 넣을 수 없습니다.")
        }
        return todayJobs
    }
}

/** 오늘의 공고 카드에 공고와 함께 보여 줄 추천 문구다. 길이는 카드 폭에서 한 줄 남짓 들어가는 만큼으로 둔다. */
data class AdminTodayJobRequest(
    @field:Positive val jobId: Long,
    @field:NotBlank @field:Size(max = 30) val recommendationTitle: String,
    @field:NotBlank @field:Size(max = 50) val recommendationDescription: String,
) {
    fun toDto(): TodayJobDto.Request = TodayJobDto.Request(
        jobId = jobId,
        recommendationTitle = recommendationTitle,
        recommendationDescription = recommendationDescription,
    )
}

/**
 * 관리자 콘솔에서 검색해 고른 공고들의 노출을 한꺼번에 바꾼다.
 * 노출 변경은 반복해도 결과가 같으므로 같은 식별자가 여러 번 와도 거절하지 않는다.
 */
data class ChangeAdminJobVisibilityRequest(
    @field:Size(min = 1, max = MAX_VISIBILITY_CHANGE_IDS) val ids: List<Long?>,
    val visibility: AdminContentVisibility,
) {
    /** 배열 요소의 제약은 Bean Validation으로 선언할 수 없어 여기서 확인한다. 요소에 null이 와도 500이 되지 않게 한다. */
    fun toCommand(): AdminJobVisibilityChangeCommand =
        AdminJobVisibilityChangeCommand(
            jobIds = ids.map { id ->
                id?.takeIf { it > 0 } ?: throw InvalidRequestFieldException("ids", "채용공고 식별자는 양수여야 합니다.")
            },
            visibility = visibility,
        )
}
