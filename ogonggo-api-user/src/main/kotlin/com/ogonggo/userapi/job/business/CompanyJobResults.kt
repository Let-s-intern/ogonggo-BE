package com.ogonggo.userapi.job.business

import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.dto.JobPageDto
import java.time.LocalDateTime

data class CompanyJobPageResult(
    val items: List<CompanyJobSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(result: JobPageDto): CompanyJobPageResult = CompanyJobPageResult(
            items = result.jobs.map(CompanyJobSummary::from),
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

/** 목록은 공고를 식별하고 게시 상태를 확인하는 데 필요한 값만 담는다. */
data class CompanyJobSummary(
    val id: Long,
    val companyName: String,
    val title: String,
    val jobField: String?,
    val jobRole: String?,
    val industry: String?,
    val employmentType: EmploymentType,
    val experienceType: ExperienceType,
    val region: String?,
    val recruitmentType: JobRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val publicationStatus: JobPublicationStatus,
    val closedAt: LocalDateTime?,
) {
    companion object {
        internal fun from(job: Job): CompanyJobSummary = CompanyJobSummary(
            id = job.requiredId(),
            companyName = job.companyName,
            title = job.title,
            jobField = job.jobField,
            jobRole = job.jobRole,
            industry = job.industry,
            employmentType = job.employmentType,
            experienceType = job.experienceType,
            region = job.region,
            recruitmentType = job.recruitmentType,
            recruitmentStartAt = job.recruitmentStartAt,
            recruitmentEndAt = job.recruitmentEndAt,
            publicationStatus = job.publicationStatus,
            closedAt = job.closedAt,
        )
    }
}

data class CompanyJobResult(
    val id: Long,
    val companyName: String,
    val parentCompanyName: String?,
    val companyLogoUrl: String?,
    val title: String,
    val jobField: String?,
    val jobRole: String?,
    val industry: String?,
    val coverImageUrl: String?,
    val employmentType: EmploymentType,
    val experienceType: ExperienceType,
    val experienceMinYears: Int?,
    val experienceMaxYears: Int?,
    val educationLevel: EducationLevel,
    val region: String?,
    val recruitmentType: JobRecruitmentType,
    val recruitmentHeadcount: Int?,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val closesWhenFilled: Boolean?,
    val autoCloseEnabled: Boolean?,
    val companyAndTeamIntroduction: String?,
    val responsibilities: String?,
    val qualifications: String?,
    val preferredQualifications: String?,
    val compensation: String?,
    val benefits: String?,
    val hiringProcess: String?,
    val recruitmentNotice: String?,
    val applicationMethod: JobApplicationMethod?,
    val sourceUrl: String?,
    val publicationStatus: JobPublicationStatus,
    val closedAt: LocalDateTime?,
) {
    companion object {
        internal fun from(job: Job): CompanyJobResult = CompanyJobResult(
            id = job.requiredId(),
            companyName = job.companyName,
            parentCompanyName = job.parentCompanyName,
            companyLogoUrl = job.companyLogoUrl,
            title = job.title,
            jobField = job.jobField,
            jobRole = job.jobRole,
            industry = job.industry,
            coverImageUrl = job.coverImageUrl,
            employmentType = job.employmentType,
            experienceType = job.experienceType,
            experienceMinYears = job.experienceMinYears,
            experienceMaxYears = job.experienceMaxYears,
            educationLevel = job.educationLevel,
            region = job.region,
            recruitmentType = job.recruitmentType,
            recruitmentHeadcount = job.recruitmentHeadcount,
            recruitmentStartAt = job.recruitmentStartAt,
            recruitmentEndAt = job.recruitmentEndAt,
            closesWhenFilled = job.closesWhenFilled,
            autoCloseEnabled = job.autoCloseEnabled,
            companyAndTeamIntroduction = job.companyAndTeamIntroduction,
            responsibilities = job.responsibilities,
            qualifications = job.qualifications,
            preferredQualifications = job.preferredQualifications,
            compensation = job.compensation,
            benefits = job.benefits,
            hiringProcess = job.hiringProcess,
            recruitmentNotice = job.recruitmentNotice,
            applicationMethod = job.applicationMethod,
            sourceUrl = job.sourceUrl,
            publicationStatus = job.publicationStatus,
            closedAt = job.closedAt,
        )
    }
}
