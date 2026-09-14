package com.ogonggo.adminapi.job.presentation.response

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.job.business.AdminJobResult
import com.ogonggo.adminapi.job.business.AdminJobSummary
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminJobSummaryResponse(
    val id: Long,
    val title: String,
    val companyName: String,
    val employmentType: EmploymentType,
    val experienceType: ExperienceType,
    val educationLevel: EducationLevel,
    val recruitmentType: JobRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val region: String?,
    val closedAt: LocalDateTime?,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
    val visibility: AdminContentVisibility,
    val source: ContentSource,
    @Schema(description = "크롤링 수집분은 null입니다.")
    val reviewStatus: ReviewStatus?,
    @Schema(description = "저장하지 않고 마감 처리 일시와 모집 종료 일시로 계산한 값입니다.")
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
            educationLevel = result.educationLevel,
            recruitmentType = result.recruitmentType,
            recruitmentStartAt = result.recruitmentStartAt,
            recruitmentEndAt = result.recruitmentEndAt,
            region = result.region,
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

data class AdminJobDetailResponse(
    val id: Long,
    val title: String,
    val companyName: String,
    val employmentType: EmploymentType,
    val experienceType: ExperienceType,
    val educationLevel: EducationLevel,
    val recruitmentType: JobRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val region: String?,
    val closedAt: LocalDateTime?,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
    val visibility: AdminContentVisibility,
    val source: ContentSource,
    @Schema(description = "크롤링 수집분은 null입니다.")
    val reviewStatus: ReviewStatus?,
    @Schema(description = "저장하지 않고 마감 처리 일시와 모집 종료 일시로 계산한 값입니다.")
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
                educationLevel = summary.educationLevel,
                recruitmentType = summary.recruitmentType,
                recruitmentStartAt = summary.recruitmentStartAt,
                recruitmentEndAt = summary.recruitmentEndAt,
                region = summary.region,
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
