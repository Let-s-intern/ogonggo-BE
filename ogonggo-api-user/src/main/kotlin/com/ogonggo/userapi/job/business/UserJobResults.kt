package com.ogonggo.userapi.job.business

import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobAnalysisContent
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.core.job.implement.dto.JobPageDto
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import java.time.LocalDateTime

/** 달력 칸과 날짜별 목록 카드를 함께 그리는 데 필요한 값만 담는다. 지표와 본문은 상세 조회로 본다. */
data class UserJobCalendarItem(
    val id: Long,
    val companyName: String,
    val title: String,
    val coverImageUrl: String?,
    val logoUrl: String?,
    val employmentType: JobEmploymentType,
    val experienceType: JobExperienceType,
    val jobField: JobField?,
    val jobRole: JobRole?,
    val recruitmentStartAt: LocalDateTime,
    val recruitmentEndAt: LocalDateTime,
    val bookmarked: Boolean,
) {
    companion object {
        internal fun from(job: Job, bookmarked: Boolean): UserJobCalendarItem = UserJobCalendarItem(
            id = job.requiredId(),
            companyName = job.companyName,
            title = job.title,
            coverImageUrl = job.coverImageUrl,
            logoUrl = job.logoUrl,
            employmentType = job.employmentType,
            experienceType = job.experienceType,
            jobField = job.jobField,
            jobRole = job.jobRole,
            recruitmentStartAt = checkNotNull(job.recruitmentStartAt) { "달력 공고의 모집 시작 일시가 없습니다." },
            recruitmentEndAt = checkNotNull(job.recruitmentEndAt) { "달력 공고의 모집 종료 일시가 없습니다." },
            bookmarked = bookmarked,
        )
    }
}

data class UserJobPageResult(
    val items: List<UserJobSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
) {
    companion object {
        internal fun from(
            result: JobPageDto,
            bookmarkedJobIds: Set<Long>,
            metrics: Map<Long, JobMetricDto>,
        ): UserJobPageResult = UserJobPageResult(
            items = result.jobs.map { job ->
                val jobId = job.requiredId()
                UserJobSummary.from(
                    job = job,
                    bookmarked = jobId in bookmarkedJobIds,
                    metric = metrics[jobId] ?: JobMetricDto.EMPTY,
                )
            },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext,
        )
    }
}

data class UserJobSummary(
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
        internal fun from(job: Job, bookmarked: Boolean, metric: JobMetricDto): UserJobSummary = UserJobSummary(
            id = job.requiredId(),
            companyName = job.companyName,
            title = job.title,
            coverImageUrl = job.coverImageUrl,
            logoUrl = job.logoUrl,
            employmentType = job.employmentType,
            experienceType = job.experienceType,
            jobField = job.jobField,
            jobRole = job.jobRole,
            experienceMinYears = job.experienceMinYears,
            educationLevel = job.educationLevel,
            region = job.region,
            subRegion = job.subRegion,
            recruitmentType = job.recruitmentType,
            recruitmentStartAt = job.recruitmentStartAt,
            recruitmentEndAt = job.recruitmentEndAt,
            closedAt = job.closedAt,
            bookmarked = bookmarked,
            viewCount = metric.viewCount,
            bookmarkCount = metric.bookmarkCount,
            commentCount = metric.commentCount,
        )
    }
}

/** 오늘의 공고 카드 한 장이다. 목록과 같은 항목에 운영자가 적은 추천 문구를 더한다. */
data class UserTodayJobSummary(
    val job: UserJobSummary,
    val recommendationTitle: String,
    val recommendationDescription: String,
)

data class UserJobResult(
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
    val recruitmentNotice: String?,
    val sourceUrl: String?,
    /** 이메일로 지원받는 공고가 지원서를 받는 주소다. */
    val applyEmail: String?,
    val closedAt: LocalDateTime?,
    val bookmarked: Boolean,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
    /** 공고 분석. 지금 본문에 대한 분석이 없으면 null이다. */
    val analysis: JobAnalysisContent?,
) {
    companion object {
        internal fun from(
            job: Job,
            bookmarked: Boolean,
            metric: JobMetricDto,
            analysis: JobAnalysisContent?,
        ): UserJobResult = UserJobResult(
            id = job.requiredId(),
            companyName = job.companyName,
            title = job.title,
            coverImageUrl = job.coverImageUrl,
            logoUrl = job.logoUrl,
            employmentType = job.employmentType,
            experienceType = job.experienceType,
            jobField = job.jobField,
            jobRole = job.jobRole,
            experienceMinYears = job.experienceMinYears,
            educationLevel = job.educationLevel,
            region = job.region,
            subRegion = job.subRegion,
            recruitmentType = job.recruitmentType,
            recruitmentStartAt = job.recruitmentStartAt,
            recruitmentEndAt = job.recruitmentEndAt,
            companyAndTeamIntroduction = job.companyAndTeamIntroduction,
            responsibilities = job.responsibilities,
            qualifications = job.qualifications,
            preferredQualifications = job.preferredQualifications,
            compensation = job.compensation,
            benefits = job.benefits,
            hiringProcess = job.hiringProcess,
            recruitmentNotice = job.recruitmentNotice,
            sourceUrl = job.sourceUrl,
            applyEmail = job.applyEmail,
            closedAt = job.closedAt,
            bookmarked = bookmarked,
            viewCount = metric.viewCount,
            bookmarkCount = metric.bookmarkCount,
            commentCount = metric.commentCount,
            analysis = analysis,
        )
    }
}

internal fun Job.requiredId(): Long = checkNotNull(id) { "채용공고 식별자가 없습니다." }
