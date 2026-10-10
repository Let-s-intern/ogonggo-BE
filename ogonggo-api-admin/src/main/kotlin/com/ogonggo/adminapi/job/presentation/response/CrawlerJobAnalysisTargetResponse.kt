package com.ogonggo.adminapi.job.presentation.response

import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobRecruitmentType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** 크롤러가 분석할 공고 하나다. 본문 칸 이름은 크롤러 채용공고 등록 요청과 같다. */
data class CrawlerJobAnalysisTargetResponse(
    val jobId: Long,
    val source: ContentSource,
    @field:Schema(description = "지금 본문의 해시. 분석을 보낼 때 그대로 돌려줍니다")
    val contentHash: String,
    val companyName: String,
    val title: String,
    val employmentType: JobEmploymentType,
    val recruitmentType: JobRecruitmentType,
    val recruitmentEndAt: LocalDateTime?,
    val sourceUrl: String?,
    val companyAndTeamIntroduction: String?,
    val responsibilities: String?,
    val qualifications: String?,
    val preferredQualifications: String?,
    val compensation: String?,
    val benefits: String?,
    val hiringProcess: String?,
    val recruitmentNotice: String?,
) {
    companion object {
        fun from(job: Job): CrawlerJobAnalysisTargetResponse = CrawlerJobAnalysisTargetResponse(
            jobId = checkNotNull(job.id) { "채용공고 식별자가 없습니다." },
            source = job.source,
            contentHash = job.contentHash(),
            companyName = job.companyName,
            title = job.title,
            employmentType = job.employmentType,
            recruitmentType = job.recruitmentType,
            recruitmentEndAt = job.recruitmentEndAt,
            sourceUrl = job.sourceUrl,
            companyAndTeamIntroduction = job.companyAndTeamIntroduction,
            responsibilities = job.responsibilities,
            qualifications = job.qualifications,
            preferredQualifications = job.preferredQualifications,
            compensation = job.compensation,
            benefits = job.benefits,
            hiringProcess = job.hiringProcess,
            recruitmentNotice = job.recruitmentNotice,
        )
    }
}
