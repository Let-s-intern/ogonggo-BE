package com.ogonggo.adminapi.job.business

import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobRecruitmentType
import java.time.LocalDateTime

/** 크롤러가 보낸 공고 한 건의 값이다. 등록과 교체가 같은 값을 쓴다. */
data class CrawlerJobCommand(
    val companyName: String,
    val parentCompanyName: String?,
    val title: String,
    val jobField: String?,
    val jobRole: String?,
    val industry: String?,
    val coverImageUrl: String?,
    val employmentType: EmploymentType,
    val experienceType: ExperienceType,
    val experienceMinYears: Int?,
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
    val sourceUrl: String,
)

data class CrawlerJobRegistrationCommand(
    val job: CrawlerJobCommand,
    val tags: List<String>,
)
