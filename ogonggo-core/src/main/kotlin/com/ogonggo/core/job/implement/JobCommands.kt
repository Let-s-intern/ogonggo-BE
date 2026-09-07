package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import java.time.LocalDateTime

data class JobAppendCommand(
    val companyName: String,
    val parentCompanyName: String? = null,
    val companyLogoUrl: String? = null,
    val title: String,
    val jobField: String? = null,
    val coverImageUrl: String? = null,
    val employmentType: EmploymentType,
    val experienceType: ExperienceType,
    val experienceMinYears: Int? = null,
    val experienceMaxYears: Int? = null,
    val educationLevel: EducationLevel = EducationLevel.ANY,
    val region: String? = null,
    val recruitmentType: JobRecruitmentType,
    val recruitmentHeadcount: Int? = null,
    val recruitmentStartAt: LocalDateTime? = null,
    val recruitmentEndAt: LocalDateTime? = null,
    val closesWhenFilled: Boolean? = null,
    val autoCloseEnabled: Boolean? = null,
    val companyAndTeamIntroduction: String? = null,
    val responsibilities: String? = null,
    val qualifications: String? = null,
    val preferredQualifications: String? = null,
    val compensation: String? = null,
    val benefits: String? = null,
    val hiringProcess: String? = null,
    val recruitmentNotice: String? = null,
    val applicationMethod: JobApplicationMethod? = null,
    val sourceUrl: String? = null,
    val publicationStatus: JobPublicationStatus = JobPublicationStatus.DRAFT,
)

data class JobUpdateCommand(
    val companyName: String,
    val parentCompanyName: String? = null,
    val companyLogoUrl: String? = null,
    val title: String,
    val jobField: String? = null,
    val coverImageUrl: String? = null,
    val employmentType: EmploymentType,
    val experienceType: ExperienceType,
    val experienceMinYears: Int? = null,
    val experienceMaxYears: Int? = null,
    val educationLevel: EducationLevel = EducationLevel.ANY,
    val region: String? = null,
    val recruitmentType: JobRecruitmentType,
    val recruitmentHeadcount: Int? = null,
    val recruitmentStartAt: LocalDateTime? = null,
    val recruitmentEndAt: LocalDateTime? = null,
    val closesWhenFilled: Boolean? = null,
    val autoCloseEnabled: Boolean? = null,
    val companyAndTeamIntroduction: String? = null,
    val responsibilities: String? = null,
    val qualifications: String? = null,
    val preferredQualifications: String? = null,
    val compensation: String? = null,
    val benefits: String? = null,
    val hiringProcess: String? = null,
    val recruitmentNotice: String? = null,
    val applicationMethod: JobApplicationMethod? = null,
    val sourceUrl: String? = null,
)
