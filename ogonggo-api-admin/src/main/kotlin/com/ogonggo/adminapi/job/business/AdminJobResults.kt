package com.ogonggo.adminapi.job.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.core.job.implement.dto.JobPageDto
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
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
        ): AdminJobPageResult = AdminJobPageResult(
            items = result.jobs.map { job ->
                AdminJobSummary.from(job, metrics[job.requiredId()] ?: JobMetricDto.EMPTY)
            },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

/** 오늘의 공고 한 건이다. 목록과 같은 항목에 운영자가 적은 추천 문구를 더한다. */
data class AdminTodayJobSummary(
    val job: AdminJobSummary,
    val recommendationTitle: String,
    val recommendationDescription: String,
)

/** 목록에는 본문 칸을 싣지 않는다. 한 페이지가 수백 KB가 되므로 본문은 상세에서만 준다. */
data class AdminJobSummary(
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
    val reviewStatus: ContentReviewStatus?,
    val recruitmentStatus: JobRecruitmentStatus,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(job: Job, metric: JobMetricDto): AdminJobSummary = AdminJobSummary(
            id = job.requiredId(),
            title = job.title,
            companyName = job.companyName,
            employmentType = job.employmentType,
            experienceType = job.experienceType,
            jobField = job.jobField,
            jobRole = job.jobRole,
            educationLevel = job.educationLevel,
            recruitmentType = job.recruitmentType,
            recruitmentStartAt = job.recruitmentStartAt,
            recruitmentEndAt = job.recruitmentEndAt,
            region = job.region,
            subRegion = job.subRegion,
            closedAt = job.closedAt,
            viewCount = metric.viewCount,
            bookmarkCount = metric.bookmarkCount,
            commentCount = metric.commentCount,
            visibility = AdminContentVisibility.of(job.publicationStatus == JobPublicationStatus.PUBLISHED),
            source = job.source,
            reviewStatus = job.reviewStatus,
            recruitmentStatus = job.recruitmentStatus,
            registeredAt = job.createdAt,
        )
    }
}

/** 목록 항목에 수정할 수 있는 공고 정보, 본문 칸과 원문 주소·원문을 더한 값이다. AI 서버가 못 읽어 온 본문 칸은 비어 있을 수 있다. */
data class AdminJobResult(
    val summary: AdminJobSummary,
    val parentCompanyName: String?,
    val industry: String?,
    val coverImageUrl: String?,
    val logoUrl: String?,
    val experienceMinYears: Int?,
    val recruitmentHeadcount: Int?,
    val closesWhenFilled: Boolean?,
    val autoCloseEnabled: Boolean?,
    val applicationMethod: JobApplicationMethod?,
    val applyEmail: String?,
    val inquiryEmail: String?,
    val companyAndTeamIntroduction: String?,
    val responsibilities: String?,
    val qualifications: String?,
    val preferredQualifications: String?,
    val compensation: String?,
    val benefits: String?,
    val hiringProcess: String?,
    val recruitmentNotice: String?,
    val sourceUrl: String?,
    /** AI 서버가 보낸 원문 HTML이다. 원문이 없는 공고는 null이다. */
    val sourceHtml: String?,
) {
    companion object {
        internal fun from(job: Job, metric: JobMetricDto, sourceHtml: String?): AdminJobResult = AdminJobResult(
            summary = AdminJobSummary.from(job, metric),
            parentCompanyName = job.parentCompanyName,
            industry = job.industry,
            coverImageUrl = job.coverImageUrl,
            logoUrl = job.logoUrl,
            experienceMinYears = job.experienceMinYears,
            recruitmentHeadcount = job.recruitmentHeadcount,
            closesWhenFilled = job.closesWhenFilled,
            autoCloseEnabled = job.autoCloseEnabled,
            applicationMethod = job.applicationMethod,
            applyEmail = job.applyEmail,
            inquiryEmail = job.inquiryEmail,
            companyAndTeamIntroduction = job.companyAndTeamIntroduction,
            responsibilities = job.responsibilities,
            qualifications = job.qualifications,
            preferredQualifications = job.preferredQualifications,
            compensation = job.compensation,
            benefits = job.benefits,
            hiringProcess = job.hiringProcess,
            recruitmentNotice = job.recruitmentNotice,
            sourceUrl = job.sourceUrl,
            sourceHtml = sourceHtml,
        )
    }
}

internal fun Job.requiredId(): Long = checkNotNull(id) { "채용공고 식별자가 없습니다." }
