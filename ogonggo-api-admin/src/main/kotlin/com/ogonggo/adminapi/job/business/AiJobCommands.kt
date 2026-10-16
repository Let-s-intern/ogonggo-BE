package com.ogonggo.adminapi.job.business

import com.ogonggo.core.job.domain.JobAnalysisContent
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import java.time.LocalDateTime

/** AI 서버가 보낸 공고 한 건의 값이다. 등록과 교체가 같은 값을 쓴다. */
data class AiJobCommand(
    val companyName: String,
    val parentCompanyName: String?,
    val title: String,
    val jobField: JobField?,
    val jobRole: JobRole?,
    val industry: String?,
    val coverImageUrl: String?,
    val logoUrl: String?,
    val employmentType: JobEmploymentType,
    val experienceType: JobExperienceType,
    val experienceMinYears: Int?,
    val educationLevel: JobEducationLevel,
    val region: Region?,
    val subRegion: SubRegion?,
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
    val applyEmail: String?,
    val inquiryEmail: String?,
    val sourceUrl: String,
    /** 원문 페이지의 본문 영역 HTML이다. 교체할 때 비워 보내면 저장된 원문을 그대로 둔다. */
    val sourceHtml: String?,
)

data class AiJobRegistrationCommand(
    val job: AiJobCommand,
    val tags: List<String>,
)

/** AI 서버가 보낸 공고 분석이다. [contentHash]는 분석한 본문의 해시다. */
data class AiJobAnalysisCommand(
    val contentHash: String,
    val guideVersion: Int?,
    val model: String,
    val content: JobAnalysisContent,
)
