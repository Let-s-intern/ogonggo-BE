package com.ogonggo.userapi.job.presentation.response

import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.userapi.job.business.CompanyJobResult
import com.ogonggo.userapi.job.business.CompanyJobSummary
import java.time.LocalDateTime

data class CreateCompanyJobResponse(val id: Long)

data class CompanyJobSummaryResponse(
    val id: Long,
    val companyName: String,
    val title: String,
    val jobField: String?,
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
        internal fun from(result: CompanyJobSummary): CompanyJobSummaryResponse = CompanyJobSummaryResponse(
            id = result.id,
            companyName = result.companyName,
            title = result.title,
            jobField = result.jobField,
            employmentType = result.employmentType,
            experienceType = result.experienceType,
            region = result.region,
            recruitmentType = result.recruitmentType,
            recruitmentStartAt = result.recruitmentStartAt,
            recruitmentEndAt = result.recruitmentEndAt,
            publicationStatus = result.publicationStatus,
            closedAt = result.closedAt,
        )
    }
}

data class CompanyJobDetailResponse(
    val id: Long,
    val companyName: String,
    val parentCompanyName: String?,
    val companyLogoUrl: String?,
    val title: String,
    val jobField: String?,
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
        internal fun from(result: CompanyJobResult): CompanyJobDetailResponse = CompanyJobDetailResponse(
            id = result.id,
            companyName = result.companyName,
            parentCompanyName = result.parentCompanyName,
            companyLogoUrl = result.companyLogoUrl,
            title = result.title,
            jobField = result.jobField,
            coverImageUrl = result.coverImageUrl,
            employmentType = result.employmentType,
            experienceType = result.experienceType,
            experienceMinYears = result.experienceMinYears,
            experienceMaxYears = result.experienceMaxYears,
            educationLevel = result.educationLevel,
            region = result.region,
            recruitmentType = result.recruitmentType,
            recruitmentHeadcount = result.recruitmentHeadcount,
            recruitmentStartAt = result.recruitmentStartAt,
            recruitmentEndAt = result.recruitmentEndAt,
            closesWhenFilled = result.closesWhenFilled,
            autoCloseEnabled = result.autoCloseEnabled,
            companyAndTeamIntroduction = result.companyAndTeamIntroduction,
            responsibilities = result.responsibilities,
            qualifications = result.qualifications,
            preferredQualifications = result.preferredQualifications,
            compensation = result.compensation,
            benefits = result.benefits,
            hiringProcess = result.hiringProcess,
            recruitmentNotice = result.recruitmentNotice,
            applicationMethod = result.applicationMethod,
            sourceUrl = result.sourceUrl,
            publicationStatus = result.publicationStatus,
            closedAt = result.closedAt,
        )
    }
}
