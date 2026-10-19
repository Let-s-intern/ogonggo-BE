package com.ogonggo.adminapi.job.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobContentField
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import java.time.LocalDateTime

/**
 * 관리자가 채용공고의 운영 값과 내용을 고친다. 넘어온 값만 바꾼다.
 * 모든 값을 늘 함께 보내게 하면 화면이 건드리지 않은 값까지 되돌려 쓰고, 그 사이 다른 곳에서 바뀐 값이 조용히 덮인다.
 */
data class AdminJobUpdateCommand(
    val visibility: AdminContentVisibility? = null,
    /** 승인이나 검수 대기로만 바꾼다. 반려는 사유가 필요해 검수 화면에서 처리한다. */
    val reviewStatus: ContentReviewStatus? = null,
    val title: String? = null,
    /** 값이 null인 칸은 비운다. */
    val contents: Map<JobContentField, String?> = emptyMap(),
    val details: AdminJobDetailsPatch = AdminJobDetailsPatch(),
)

/**
 * 제목·본문 밖의 공고 정보(경력, 모집 기간, 직군·직무, 지역 등)다. null인 값은 그대로 두고,
 * 값을 비우려면 [cleared]에 담는다. JSON에서 "보내지 않음"과 "null"을 가를 수 없어 비우기를 따로 받는다.
 */
data class AdminJobDetailsPatch(
    val companyName: String? = null,
    val parentCompanyName: String? = null,
    val industry: String? = null,
    val coverImageUrl: String? = null,
    val logoUrl: String? = null,
    val jobField: JobField? = null,
    val jobRole: JobRole? = null,
    val employmentType: JobEmploymentType? = null,
    val experienceType: JobExperienceType? = null,
    val experienceMinYears: Int? = null,
    val educationLevel: JobEducationLevel? = null,
    val region: Region? = null,
    val subRegion: SubRegion? = null,
    val recruitmentType: JobRecruitmentType? = null,
    val recruitmentHeadcount: Int? = null,
    val recruitmentStartAt: LocalDateTime? = null,
    val recruitmentEndAt: LocalDateTime? = null,
    val closesWhenFilled: Boolean? = null,
    val autoCloseEnabled: Boolean? = null,
    val recruitmentNotice: String? = null,
    val applicationMethod: JobApplicationMethod? = null,
    val applyEmail: String? = null,
    val inquiryEmail: String? = null,
    val sourceUrl: String? = null,
    val cleared: Set<AdminJobClearableField> = emptySet(),
) {
    fun isEmpty(): Boolean = this == EMPTY

    private companion object {
        val EMPTY = AdminJobDetailsPatch()
    }
}

/** 관리자가 비울 수 있는 공고 정보다. 필수 값(회사명, 고용 형태, 경력 유형, 학력, 모집 기간 유형)은 비울 수 없다. */
enum class AdminJobClearableField(val fieldName: String) {
    PARENT_COMPANY_NAME("parentCompanyName"),
    INDUSTRY("industry"),
    COVER_IMAGE_URL("coverImageUrl"),
    LOGO_URL("logoUrl"),
    JOB_FIELD("jobField"),
    JOB_ROLE("jobRole"),
    EXPERIENCE_MIN_YEARS("experienceMinYears"),
    REGION("region"),
    SUB_REGION("subRegion"),
    RECRUITMENT_HEADCOUNT("recruitmentHeadcount"),
    RECRUITMENT_START_AT("recruitmentStartAt"),
    RECRUITMENT_END_AT("recruitmentEndAt"),
    CLOSES_WHEN_FILLED("closesWhenFilled"),
    AUTO_CLOSE_ENABLED("autoCloseEnabled"),
    RECRUITMENT_NOTICE("recruitmentNotice"),
    APPLICATION_METHOD("applicationMethod"),
    APPLY_EMAIL("applyEmail"),
    INQUIRY_EMAIL("inquiryEmail"),
    SOURCE_URL("sourceUrl"),
    ;

    companion object {
        fun fromFieldName(fieldName: String): AdminJobClearableField? = entries.firstOrNull { it.fieldName == fieldName }
    }
}

/** 관리자가 고른 채용공고들의 노출을 한꺼번에 바꾼다. 하나라도 바꿀 수 없으면 아무것도 바꾸지 않는다. */
data class AdminJobVisibilityChangeCommand(
    val jobIds: List<Long>,
    val visibility: AdminContentVisibility,
)
