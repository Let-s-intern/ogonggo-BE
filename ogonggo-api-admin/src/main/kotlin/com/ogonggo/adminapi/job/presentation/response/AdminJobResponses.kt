package com.ogonggo.adminapi.job.presentation.response

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.job.business.AdminJobResult
import com.ogonggo.adminapi.job.business.AdminJobSummary
import com.ogonggo.adminapi.job.business.AdminTodayJobSummary
import com.ogonggo.adminapi.job.presentation.request.AiJobAnalysisRequest
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminJobSummaryResponse(
    val id: Long,
    val title: String,
    val companyName: String,
    val employmentType: JobEmploymentType,
    val experienceType: JobExperienceType,
    val jobField: JobField?,
    val jobRole: JobRole?,
    val educationLevel: JobEducationLevel,
    val recruitmentType: JobRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val region: Region?,
    val subRegion: SubRegion?,
    val closedAt: LocalDateTime?,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
    val visibility: AdminContentVisibility,
    val source: ContentSource,
    @Schema(description = "크롤링·고용24 수집분은 null입니다.")
    val reviewStatus: ContentReviewStatus?,
    @Schema(description = "저장된 모집 상태입니다. 모집 종료 일시가 지난 공고는 매시 정각에 CLOSED로 바뀝니다.")
    val recruitmentStatus: JobRecruitmentStatus,
    val registeredAt: LocalDateTime,
    @Schema(description = "검수 완료 일시(한국 시간). 미검수면 null입니다. 기업회원 공고의 검수 상태(reviewStatus)와 다른 값입니다.")
    val inspectedAt: LocalDateTime?,
    @Schema(description = "공고 원문 주소입니다. 원문 주소가 없는 공고는 null입니다.")
    val sourceUrl: String?,
    @Schema(description = PRIORITY_EXPOSED_DESCRIPTION)
    val priorityExposed: Boolean,
) {
    companion object {
        internal fun from(result: AdminJobSummary): AdminJobSummaryResponse = AdminJobSummaryResponse(
            id = result.id,
            title = result.title,
            companyName = result.companyName,
            employmentType = result.employmentType,
            experienceType = result.experienceType,
            jobField = result.jobField,
            jobRole = result.jobRole,
            educationLevel = result.educationLevel,
            recruitmentType = result.recruitmentType,
            recruitmentStartAt = result.recruitmentStartAt,
            recruitmentEndAt = result.recruitmentEndAt,
            region = result.region,
            subRegion = result.subRegion,
            closedAt = result.closedAt,
            viewCount = result.viewCount,
            bookmarkCount = result.bookmarkCount,
            commentCount = result.commentCount,
            visibility = result.visibility,
            source = result.source,
            reviewStatus = result.reviewStatus,
            recruitmentStatus = result.recruitmentStatus,
            registeredAt = result.registeredAt,
            inspectedAt = result.inspectedAt,
            sourceUrl = result.sourceUrl,
            priorityExposed = result.priorityExposed,
        )
    }
}

/** 목록 항목과 같은 필드를 같은 높이에 펼치고 추천 문구를 더한다. 콘솔이 목록과 같은 표로 그리기 위해서다. */
data class AdminTodayJobSummaryResponse(
    @get:JsonUnwrapped
    val job: AdminJobSummaryResponse,
    @field:Schema(description = "사용자 화면 카드에 굵게 보여 주는 추천 문구 제목")
    val recommendationTitle: String,
    @field:Schema(description = "추천 문구 제목 아래 설명")
    val recommendationDescription: String,
) {
    companion object {
        internal fun from(result: AdminTodayJobSummary): AdminTodayJobSummaryResponse = AdminTodayJobSummaryResponse(
            job = AdminJobSummaryResponse.from(result.job),
            recommendationTitle = result.recommendationTitle,
            recommendationDescription = result.recommendationDescription,
        )
    }
}

data class AdminJobDetailResponse(
    val id: Long,
    val title: String,
    val companyName: String,
    val parentCompanyName: String?,
    val industry: String?,
    val coverImageUrl: String?,
    val logoUrl: String?,
    val employmentType: JobEmploymentType,
    val experienceType: JobExperienceType,
    @Schema(description = "최소 요구 경력 연수입니다. 공고에 없으면 null입니다.")
    val experienceMinYears: Int?,
    val jobField: JobField?,
    val jobRole: JobRole?,
    val educationLevel: JobEducationLevel,
    val recruitmentType: JobRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val recruitmentHeadcount: Int?,
    val closesWhenFilled: Boolean?,
    val autoCloseEnabled: Boolean?,
    val region: Region?,
    val subRegion: SubRegion?,
    val closedAt: LocalDateTime?,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
    val visibility: AdminContentVisibility,
    val source: ContentSource,
    @Schema(description = "크롤링·고용24 수집분은 null입니다.")
    val reviewStatus: ContentReviewStatus?,
    @Schema(description = "저장된 모집 상태입니다. 모집 종료 일시가 지난 공고는 매시 정각에 CLOSED로 바뀝니다.")
    val recruitmentStatus: JobRecruitmentStatus,
    val registeredAt: LocalDateTime,
    @Schema(description = "검수 완료 일시(한국 시간). 미검수면 null입니다. 기업회원 공고의 검수 상태(reviewStatus)와 다른 값입니다.")
    val inspectedAt: LocalDateTime?,
    @Schema(description = PRIORITY_EXPOSED_DESCRIPTION)
    val priorityExposed: Boolean,
    val companyAndTeamIntroduction: String?,
    val responsibilities: String?,
    val qualifications: String?,
    val preferredQualifications: String?,
    val compensation: String?,
    val benefits: String?,
    val hiringProcess: String?,
    val recruitmentNotice: String?,
    val applicationMethod: JobApplicationMethod?,
    val applyEmail: String?,
    val inquiryEmail: String?,
    val sourceUrl: String?,
    @Schema(description = "채용공고 원문 HTML입니다. 원문 페이지의 본문 영역을 안전한 태그만 남긴 것이며, 원문이 없는 공고와 기업회원 공고는 null입니다.")
    val sourceHtml: String?,
    @Schema(description = "저장된 공고 분석입니다. 사용자 상세의 analysis와 같은 칸이며 PUT /api/v1/admin/jobs/{jobId}/analysis 본문과도 같습니다. 사용자 상세와 달리 본문이 바뀌어도 저장된 분석을 싣고 analysisCurrent로 알립니다. 분석이 없으면 null입니다.")
    val analysis: AiJobAnalysisRequest.Analysis?,
    @Schema(description = "analysis가 지금 본문(제목과 본문 여덟 칸)으로 만든 것인지입니다. false면 사용자에게 보이지 않고 AI 서버의 다시 분석을 기다립니다. 분석이 없으면 false입니다.")
    val analysisCurrent: Boolean,
    @Schema(description = "운영자가 고친 분석이면 고친 일시(한국 시간)입니다. AI 서버가 만든 분석이거나 분석이 없으면 null입니다. 본문이 바뀌기 전까지 AI 서버는 운영자가 고친 분석을 덮어쓰지 않습니다.")
    val analysisEditedAt: LocalDateTime?,
    @Schema(description = "운영자가 공고 내용(제목·본문·공고 정보·원문)을 마지막으로 고친 일시(한국 시간)입니다. 값이 있으면 AI 서버가 다시 수집해도 공고 내용과 원문을 바꾸지 않습니다. 고친 적이 없으면 null입니다.")
    val contentEditedAt: LocalDateTime?,
) {
    companion object {
        internal fun from(result: AdminJobResult): AdminJobDetailResponse {
            val summary = result.summary
            return AdminJobDetailResponse(
                id = summary.id,
                title = summary.title,
                companyName = summary.companyName,
                parentCompanyName = result.parentCompanyName,
                industry = result.industry,
                coverImageUrl = result.coverImageUrl,
                logoUrl = result.logoUrl,
                employmentType = summary.employmentType,
                experienceType = summary.experienceType,
                experienceMinYears = result.experienceMinYears,
                jobField = summary.jobField,
                jobRole = summary.jobRole,
                educationLevel = summary.educationLevel,
                recruitmentType = summary.recruitmentType,
                recruitmentStartAt = summary.recruitmentStartAt,
                recruitmentEndAt = summary.recruitmentEndAt,
                recruitmentHeadcount = result.recruitmentHeadcount,
                closesWhenFilled = result.closesWhenFilled,
                autoCloseEnabled = result.autoCloseEnabled,
                region = summary.region,
                subRegion = summary.subRegion,
                closedAt = summary.closedAt,
                viewCount = summary.viewCount,
                bookmarkCount = summary.bookmarkCount,
                commentCount = summary.commentCount,
                visibility = summary.visibility,
                source = summary.source,
                reviewStatus = summary.reviewStatus,
                recruitmentStatus = summary.recruitmentStatus,
                registeredAt = summary.registeredAt,
                inspectedAt = summary.inspectedAt,
                priorityExposed = summary.priorityExposed,
                companyAndTeamIntroduction = result.companyAndTeamIntroduction,
                responsibilities = result.responsibilities,
                qualifications = result.qualifications,
                preferredQualifications = result.preferredQualifications,
                compensation = result.compensation,
                benefits = result.benefits,
                hiringProcess = result.hiringProcess,
                recruitmentNotice = result.recruitmentNotice,
                applicationMethod = result.applicationMethod,
                applyEmail = result.applyEmail,
                inquiryEmail = result.inquiryEmail,
                sourceUrl = summary.sourceUrl,
                sourceHtml = result.sourceHtml,
                analysis = result.analysis?.let(AiJobAnalysisRequest.Analysis::from),
                analysisCurrent = result.analysisCurrent,
                analysisEditedAt = result.analysisEditedAt,
                contentEditedAt = result.contentEditedAt,
            )
        }
    }
}

private const val PRIORITY_EXPOSED_DESCRIPTION =
    "기업회원 공고 우선 노출을 이 공고에 줄지입니다. 기업회원 공고(source = COMPANY)에만 영향이 있고, " +
        "다른 경로 공고는 저장한 값(기본 true)을 그대로 보여 줍니다. 전체 설정(company-content-priority)이 켜져 있고 " +
        "이 값이 true인 마감 전 공고만 공개 목록 맨 앞에 둡니다."
