package com.ogonggo.adminapi.job.presentation.request

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.adminapi.job.business.AdminJobClearableField
import com.ogonggo.adminapi.job.business.AdminJobDetailsPatch
import com.ogonggo.adminapi.job.business.AdminJobUpdateCommand
import com.ogonggo.adminapi.job.business.AdminJobVisibilityChangeCommand
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobContentField
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import io.swagger.v3.oas.annotations.media.Schema
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.job.implement.dto.TodayJobDto
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import org.hibernate.validator.constraints.URL

/** 콘솔 목록 한 페이지(최대 100건)를 여러 장 골라도 넉넉하고, 한 트랜잭션의 잠금이 지나치게 길어지지 않을 만큼으로 둔다. */
private const val MAX_VISIBILITY_CHANGE_IDS = 1000

/**
 * 모든 값이 선택이며 넘어온 값만 바꾼다. 등록 경로(`source`)는 바꿀 수 없어 받지 않는다.
 *
 * `fields`는 허용한 본문 칸만 반영하고 목록 밖의 키는 버린다.
 * 빈 문자열은 그 칸을 비우라는 뜻이다.
 *
 * 공고 정보(회사명부터 `sourceUrl`까지)는 null이면 그대로 두고, 비우려면 필드 이름을 `clear`에 담는다.
 */
data class UpdateAdminJobRequest(
    val visibility: AdminContentVisibility?,
    val reviewStatus: ContentReviewStatus?,
    @field:Size(max = 255) val title: String?,
    val fields: Map<String, String?>?,
    @field:Size(max = 150) val companyName: String? = null,
    @field:Size(max = 150) val parentCompanyName: String? = null,
    @field:Size(max = 100) val industry: String? = null,
    @field:Size(max = 2048) @field:URL val coverImageUrl: String? = null,
    @field:Size(max = 2048) @field:URL val logoUrl: String? = null,
    @field:Schema(description = "직군입니다. 바꾸면서 jobRole을 보내지 않으면 다른 직군의 기존 직무는 지웁니다.")
    val jobField: JobField? = null,
    @field:Schema(description = "직무입니다. jobField 없이 보내면 직군도 이 직무의 직군으로 바꿉니다.")
    val jobRole: JobRole? = null,
    val employmentType: JobEmploymentType? = null,
    val experienceType: JobExperienceType? = null,
    @field:PositiveOrZero val experienceMinYears: Int? = null,
    val educationLevel: JobEducationLevel? = null,
    @field:Schema(description = "근무 시·도입니다. 바꾸면서 subRegion을 보내지 않으면 다른 시·도의 기존 시·군·구는 지웁니다.")
    val region: Region? = null,
    @field:Schema(description = "근무 시·군·구입니다. region 없이 보내면 시·도도 이 시·군·구의 시·도로 바꿉니다.")
    val subRegion: SubRegion? = null,
    @field:Schema(description = "모집 기간 유형입니다. ALWAYS_OPEN으로 바꾸면 모집 종료 일시를 지웁니다.")
    val recruitmentType: JobRecruitmentType? = null,
    @field:Positive val recruitmentHeadcount: Int? = null,
    val recruitmentStartAt: LocalDateTime? = null,
    val recruitmentEndAt: LocalDateTime? = null,
    val closesWhenFilled: Boolean? = null,
    val autoCloseEnabled: Boolean? = null,
    val recruitmentNotice: String? = null,
    val applicationMethod: JobApplicationMethod? = null,
    @field:Size(max = 320) @field:Email val applyEmail: String? = null,
    @field:Size(max = 320) @field:Email val inquiryEmail: String? = null,
    @field:Size(max = 2048) @field:URL val sourceUrl: String? = null,
    @field:Schema(
        description = "비울 공고 정보의 필드 이름입니다. parentCompanyName, industry, coverImageUrl, logoUrl, jobField, jobRole, " +
            "experienceMinYears, region, subRegion, recruitmentHeadcount, recruitmentStartAt, recruitmentEndAt, closesWhenFilled, " +
            "autoCloseEnabled, recruitmentNotice, applicationMethod, applyEmail, inquiryEmail, sourceUrl을 보낼 수 있습니다.",
    )
    val clear: List<String>? = null,
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
            details = toDetailsPatch(),
        )
    }

    private fun toDetailsPatch(): AdminJobDetailsPatch {
        val cleared = clear.orEmpty().map { name ->
            AdminJobClearableField.fromFieldName(name)
                ?: throw InvalidRequestFieldException("clear", "비울 수 없는 필드입니다: $name")
        }.toSet()
        val sent = mapOf(
            AdminJobClearableField.PARENT_COMPANY_NAME to parentCompanyName,
            AdminJobClearableField.INDUSTRY to industry,
            AdminJobClearableField.COVER_IMAGE_URL to coverImageUrl,
            AdminJobClearableField.LOGO_URL to logoUrl,
            AdminJobClearableField.JOB_FIELD to jobField,
            AdminJobClearableField.JOB_ROLE to jobRole,
            AdminJobClearableField.EXPERIENCE_MIN_YEARS to experienceMinYears,
            AdminJobClearableField.REGION to region,
            AdminJobClearableField.SUB_REGION to subRegion,
            AdminJobClearableField.RECRUITMENT_HEADCOUNT to recruitmentHeadcount,
            AdminJobClearableField.RECRUITMENT_START_AT to recruitmentStartAt,
            AdminJobClearableField.RECRUITMENT_END_AT to recruitmentEndAt,
            AdminJobClearableField.CLOSES_WHEN_FILLED to closesWhenFilled,
            AdminJobClearableField.AUTO_CLOSE_ENABLED to autoCloseEnabled,
            AdminJobClearableField.RECRUITMENT_NOTICE to recruitmentNotice,
            AdminJobClearableField.APPLICATION_METHOD to applicationMethod,
            AdminJobClearableField.APPLY_EMAIL to applyEmail,
            AdminJobClearableField.INQUIRY_EMAIL to inquiryEmail,
            AdminJobClearableField.SOURCE_URL to sourceUrl,
        )
        cleared.firstOrNull { sent[it] != null }?.let { field ->
            throw InvalidRequestFieldException(field.fieldName, "값을 보내면서 비울 수 없습니다.")
        }
        return AdminJobDetailsPatch(
            companyName = notBlank("companyName", companyName),
            parentCompanyName = notBlank("parentCompanyName", parentCompanyName),
            industry = notBlank("industry", industry),
            coverImageUrl = notBlank("coverImageUrl", coverImageUrl),
            logoUrl = notBlank("logoUrl", logoUrl),
            jobField = jobField,
            jobRole = jobRole,
            employmentType = employmentType,
            experienceType = experienceType,
            experienceMinYears = experienceMinYears,
            educationLevel = educationLevel,
            region = region,
            subRegion = subRegion,
            recruitmentType = recruitmentType,
            recruitmentHeadcount = recruitmentHeadcount,
            recruitmentStartAt = recruitmentStartAt,
            recruitmentEndAt = recruitmentEndAt,
            closesWhenFilled = closesWhenFilled,
            autoCloseEnabled = autoCloseEnabled,
            recruitmentNotice = notBlank("recruitmentNotice", recruitmentNotice),
            applicationMethod = applicationMethod,
            applyEmail = notBlank("applyEmail", applyEmail),
            inquiryEmail = notBlank("inquiryEmail", inquiryEmail),
            sourceUrl = notBlank("sourceUrl", sourceUrl),
            cleared = cleared,
        )
    }
}

/** 빈 문자열은 `@Email`·`@URL`을 통과하지만 도메인은 공백 값을 막는다. 비우기는 `clear`로 받는다. */
private fun notBlank(field: String, value: String?): String? {
    if (value != null && value.isBlank()) {
        throw InvalidRequestFieldException(field, "공백일 수 없습니다. 비우려면 clear에 담아 주세요.")
    }
    return value
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

/** 오늘의 공고 카드에 공고와 함께 보여 줄 추천 문구다. 제목은 카드 폭에서 한 줄 남짓, 설명은 여러 줄로 200자까지 둔다. */
data class AdminTodayJobRequest(
    @field:Positive val jobId: Long,
    @field:NotBlank @field:Size(max = 30) val recommendationTitle: String,
    @field:NotBlank @field:Size(max = 200) val recommendationDescription: String,
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
