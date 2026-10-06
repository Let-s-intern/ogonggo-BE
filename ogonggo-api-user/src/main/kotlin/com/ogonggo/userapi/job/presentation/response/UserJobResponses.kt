package com.ogonggo.userapi.job.presentation.response

import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import com.ogonggo.userapi.job.business.UserJobCalendarItem
import com.ogonggo.userapi.job.business.UserJobResult
import com.ogonggo.userapi.job.business.UserJobSummary
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class UserJobCalendarItemResponse(
    val id: Long,
    val companyName: String,
    val title: String,
    @field:Schema(description = "공고 대표 이미지 주소. 없으면 null")
    val coverImageUrl: String?,
    @field:Schema(description = "기업 로고 이미지 주소. 없으면 null")
    val logoUrl: String?,
    val employmentType: JobEmploymentType,
    val experienceType: JobExperienceType,
    val jobField: JobField?,
    val jobRole: JobRole?,
    val recruitmentStartAt: LocalDateTime,
    val recruitmentEndAt: LocalDateTime,
    @field:Schema(description = "로그인한 사용자의 북마크 여부. 토큰이 없으면 항상 false")
    val bookmarked: Boolean,
) {
    companion object {
        internal fun from(result: UserJobCalendarItem): UserJobCalendarItemResponse = UserJobCalendarItemResponse(
            id = result.id,
            companyName = result.companyName,
            title = result.title,
            coverImageUrl = result.coverImageUrl,
            logoUrl = result.logoUrl,
            employmentType = result.employmentType,
            experienceType = result.experienceType,
            jobField = result.jobField,
            jobRole = result.jobRole,
            recruitmentStartAt = result.recruitmentStartAt,
            recruitmentEndAt = result.recruitmentEndAt,
            bookmarked = result.bookmarked,
        )
    }
}

data class UserJobSummaryResponse(
    val id: Long,
    val companyName: String,
    val title: String,
    val coverImageUrl: String?,
    val logoUrl: String?,
    val employmentType: JobEmploymentType,
    val experienceType: JobExperienceType,
    val jobField: JobField?,
    val jobRole: JobRole?,
    val experienceMinYears: Int?,
    val educationLevel: JobEducationLevel,
    val region: Region?,
    val subRegion: SubRegion?,
    val recruitmentType: JobRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val closedAt: LocalDateTime?,
    val bookmarked: Boolean,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
) {
    companion object {
        internal fun from(result: UserJobSummary): UserJobSummaryResponse = UserJobSummaryResponse(
            id = result.id,
            companyName = result.companyName,
            title = result.title,
            coverImageUrl = result.coverImageUrl,
            logoUrl = result.logoUrl,
            employmentType = result.employmentType,
            experienceType = result.experienceType,
            jobField = result.jobField,
            jobRole = result.jobRole,
            experienceMinYears = result.experienceMinYears,
            educationLevel = result.educationLevel,
            region = result.region,
            subRegion = result.subRegion,
            recruitmentType = result.recruitmentType,
            recruitmentStartAt = result.recruitmentStartAt,
            recruitmentEndAt = result.recruitmentEndAt,
            closedAt = result.closedAt,
            bookmarked = result.bookmarked,
            viewCount = result.viewCount,
            bookmarkCount = result.bookmarkCount,
            commentCount = result.commentCount,
        )
    }
}

data class UserJobDetailResponse(
    val id: Long,
    val companyName: String,
    val title: String,
    val coverImageUrl: String?,
    val logoUrl: String?,
    val employmentType: JobEmploymentType,
    val experienceType: JobExperienceType,
    val jobField: JobField?,
    val jobRole: JobRole?,
    val experienceMinYears: Int?,
    val educationLevel: JobEducationLevel,
    val region: Region?,
    val subRegion: SubRegion?,
    val recruitmentType: JobRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val companyAndTeamIntroduction: String?,
    val responsibilities: String?,
    val qualifications: String?,
    val preferredQualifications: String?,
    val compensation: String?,
    val benefits: String?,
    val hiringProcess: String?,
    val sourceUrl: String?,
    val applyEmail: String?,
    val closedAt: LocalDateTime?,
    val bookmarked: Boolean,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
) {
    companion object {
        internal fun from(result: UserJobResult): UserJobDetailResponse = UserJobDetailResponse(
            id = result.id,
            companyName = result.companyName,
            title = result.title,
            coverImageUrl = result.coverImageUrl,
            logoUrl = result.logoUrl,
            employmentType = result.employmentType,
            experienceType = result.experienceType,
            jobField = result.jobField,
            jobRole = result.jobRole,
            experienceMinYears = result.experienceMinYears,
            educationLevel = result.educationLevel,
            region = result.region,
            subRegion = result.subRegion,
            recruitmentType = result.recruitmentType,
            recruitmentStartAt = result.recruitmentStartAt,
            recruitmentEndAt = result.recruitmentEndAt,
            companyAndTeamIntroduction = result.companyAndTeamIntroduction,
            responsibilities = result.responsibilities,
            qualifications = result.qualifications,
            preferredQualifications = result.preferredQualifications,
            compensation = result.compensation,
            benefits = result.benefits,
            hiringProcess = result.hiringProcess,
            sourceUrl = result.sourceUrl,
            applyEmail = result.applyEmail,
            closedAt = result.closedAt,
            bookmarked = result.bookmarked,
            viewCount = result.viewCount,
            bookmarkCount = result.bookmarkCount,
            commentCount = result.commentCount,
        )
    }
}
