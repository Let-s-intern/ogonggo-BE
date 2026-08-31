package com.ogonggo.adminapi.job.business

import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import java.time.LocalDateTime

data class CrawlerJobRegistrationCommand(
    val companyName: String,
    val parentCompanyName: String?,
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
    val sourceUrl: String,
    val tags: List<String>,
    val publicationStatus: JobPublicationStatus,
)
