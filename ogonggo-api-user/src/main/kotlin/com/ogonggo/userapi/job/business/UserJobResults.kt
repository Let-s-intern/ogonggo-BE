package com.ogonggo.userapi.job.business

import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.JobMetricData
import com.ogonggo.core.job.implement.JobPage
import java.time.LocalDateTime

data class UserJobCalendarItem(
    val id: Long,
    val companyName: String,
    val recruitmentStartAt: LocalDateTime,
    val recruitmentEndAt: LocalDateTime,
) {
    companion object {
        internal fun from(job: Job): UserJobCalendarItem = UserJobCalendarItem(
            id = job.requiredId(),
            companyName = job.companyName,
            recruitmentStartAt = checkNotNull(job.recruitmentStartAt) { "달력 공고의 모집 시작 일시가 없습니다." },
            recruitmentEndAt = checkNotNull(job.recruitmentEndAt) { "달력 공고의 모집 종료 일시가 없습니다." },
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
            result: JobPage,
            bookmarkedJobIds: Set<Long>,
            metrics: Map<Long, JobMetricData>,
        ): UserJobPageResult = UserJobPageResult(
            items = result.jobs.map { job ->
                val jobId = job.requiredId()
                UserJobSummary.from(
                    job = job,
                    bookmarked = jobId in bookmarkedJobIds,
                    metric = metrics[jobId] ?: JobMetricData.EMPTY,
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
    val employmentType: EmploymentType,
    val experienceType: ExperienceType,
    val experienceMinYears: Int?,
    val experienceMaxYears: Int?,
    val educationLevel: EducationLevel,
    val region: String?,
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
        internal fun from(job: Job, bookmarked: Boolean, metric: JobMetricData): UserJobSummary = UserJobSummary(
            id = job.requiredId(),
            companyName = job.companyName,
            title = job.title,
            employmentType = job.employmentType,
            experienceType = job.experienceType,
            experienceMinYears = job.experienceMinYears,
            experienceMaxYears = job.experienceMaxYears,
            educationLevel = job.educationLevel,
            region = job.region,
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

data class UserJobResult(
    val id: Long,
    val companyName: String,
    val title: String,
    val employmentType: EmploymentType,
    val experienceType: ExperienceType,
    val experienceMinYears: Int?,
    val experienceMaxYears: Int?,
    val educationLevel: EducationLevel,
    val region: String?,
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
    val closedAt: LocalDateTime?,
    val bookmarked: Boolean,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
) {
    companion object {
        internal fun from(job: Job, bookmarked: Boolean, metric: JobMetricData): UserJobResult = UserJobResult(
            id = job.requiredId(),
            companyName = job.companyName,
            title = job.title,
            employmentType = job.employmentType,
            experienceType = job.experienceType,
            experienceMinYears = job.experienceMinYears,
            experienceMaxYears = job.experienceMaxYears,
            educationLevel = job.educationLevel,
            region = job.region,
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
            sourceUrl = job.sourceUrl,
            closedAt = job.closedAt,
            bookmarked = bookmarked,
            viewCount = metric.viewCount,
            bookmarkCount = metric.bookmarkCount,
            commentCount = metric.commentCount,
        )
    }
}

internal fun Job.requiredId(): Long = checkNotNull(id) { "채용공고 식별자가 없습니다." }
