package com.ogonggo.adminapi.job.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.core.job.implement.dto.JobPageDto
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus
import java.time.LocalDateTime

data class AdminJobPageResult(
    val items: List<AdminJobSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(
            result: JobPageDto,
            metrics: Map<Long, JobMetricDto>,
            now: LocalDateTime,
        ): AdminJobPageResult = AdminJobPageResult(
            items = result.jobs.map { job ->
                AdminJobSummary.from(job, metrics[job.requiredId()] ?: JobMetricDto.EMPTY, now)
            },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

/** 목록에는 본문 칸을 싣지 않는다. 한 페이지가 수백 KB가 되므로 본문은 상세에서만 준다. */
data class AdminJobSummary(
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
    val reviewStatus: ReviewStatus?,
    val recruitmentStatus: JobRecruitmentStatus,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(job: Job, metric: JobMetricDto, now: LocalDateTime): AdminJobSummary = AdminJobSummary(
            id = job.requiredId(),
            title = job.title,
            companyName = job.companyName,
            employmentType = job.employmentType,
            experienceType = job.experienceType,
            educationLevel = job.educationLevel,
            recruitmentType = job.recruitmentType,
            recruitmentStartAt = job.recruitmentStartAt,
            recruitmentEndAt = job.recruitmentEndAt,
            region = job.region,
            closedAt = job.closedAt,
            viewCount = metric.viewCount,
            bookmarkCount = metric.bookmarkCount,
            commentCount = metric.commentCount,
            visibility = AdminContentVisibility.of(job.publicationStatus == JobPublicationStatus.PUBLISHED),
            source = ContentSource.of(job.ownerUserId),
            reviewStatus = job.reviewStatus,
            recruitmentStatus = job.recruitmentStatus(now),
            registeredAt = job.createdAt,
        )
    }
}

/** 목록 항목에 본문 칸과 원문 주소를 더한 값이다. 크롤러가 못 읽어 온 본문 칸은 비어 있을 수 있다. */
data class AdminJobResult(
    val summary: AdminJobSummary,
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
        internal fun from(job: Job, metric: JobMetricDto, now: LocalDateTime): AdminJobResult = AdminJobResult(
            summary = AdminJobSummary.from(job, metric, now),
            companyAndTeamIntroduction = job.companyAndTeamIntroduction,
            responsibilities = job.responsibilities,
            qualifications = job.qualifications,
            preferredQualifications = job.preferredQualifications,
            compensation = job.compensation,
            benefits = job.benefits,
            hiringProcess = job.hiringProcess,
            sourceUrl = job.sourceUrl,
        )
    }
}

internal fun Job.requiredId(): Long = checkNotNull(id) { "채용공고 식별자가 없습니다." }
