package com.ogonggo.adminapi.job.presentation.response

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.job.business.AdminJobResult
import com.ogonggo.adminapi.job.business.AdminJobSummary
import com.ogonggo.adminapi.job.business.AdminTodayJobSummary
import com.fasterxml.jackson.annotation.JsonUnwrapped
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
    val companyAndTeamIntroduction: String?,
    val responsibilities: String?,
    val qualifications: String?,
    val preferredQualifications: String?,
    val compensation: String?,
    val benefits: String?,
    val hiringProcess: String?,
    val sourceUrl: String?,
) {
    companion object {
        internal fun from(result: AdminJobResult): AdminJobDetailResponse {
            val summary = result.summary
            return AdminJobDetailResponse(
                id = summary.id,
                title = summary.title,
                companyName = summary.companyName,
                employmentType = summary.employmentType,
                experienceType = summary.experienceType,
                jobField = summary.jobField,
                jobRole = summary.jobRole,
                educationLevel = summary.educationLevel,
                recruitmentType = summary.recruitmentType,
                recruitmentStartAt = summary.recruitmentStartAt,
                recruitmentEndAt = summary.recruitmentEndAt,
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
                companyAndTeamIntroduction = result.companyAndTeamIntroduction,
                responsibilities = result.responsibilities,
                qualifications = result.qualifications,
                preferredQualifications = result.preferredQualifications,
                compensation = result.compensation,
                benefits = result.benefits,
                hiringProcess = result.hiringProcess,
                sourceUrl = result.sourceUrl,
            )
        }
    }
}
